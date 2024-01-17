package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"strings"
	"sync"

	"github.com/gorilla/websocket"
	"github.com/nats-io/nats.go"
)

// Client represents a WebSocket client.
type Client struct {
	conn         *websocket.Conn
	sessionID    int
	symbols      []string
	messageQueue chan StockTick // Per-client message queue
}

var (
	clients      = make(map[*Client]bool)
	clientsMutex sync.Mutex
	sessionID    int
)

func main() {
	// Connect to NATS server
	nc, err := nats.Connect("nats://localhost:4222")
	if err != nil {
		log.Fatal(err)
	}
	defer nc.Close()

	// Set up WebSocket handler
	http.HandleFunc("/alladin/ticks", handleWebSocket)

	// Set up REST API handler for getting details of connected clients
	http.HandleFunc("/alladin/current-ticks-sessions", handleGetClients)

	// Start listening for WebSocket connections and REST API requests
	go http.ListenAndServe(":8080", nil)

	// Subscribe to NATS subject "stock.ticks.*"
	_, err = nc.Subscribe("stock.ticks.*", func(msg *nats.Msg) {
		handleNATSMessage(msg.Data)
	})
	if err != nil {
		log.Fatal(err)
	}

	// for {
	// 	log.Printf("Number of live goroutines: %d", runtime.NumGoroutine())
	// 	time.Sleep(time.Second)
	// }

	// Keep the main goroutine alive
	select {}
}

func handleWebSocket(w http.ResponseWriter, r *http.Request) {
	// Upgrade the HTTP connection to a WebSocket connection
	conn, err := websocket.Upgrade(w, r, nil, 1024, 1024)
	if err != nil {
		log.Print(err)
		return
	}

	// Create a new WebSocket client with a unique session ID
	client := &Client{
		conn:         conn,
		sessionID:    nextSessionID(),
		messageQueue: make(chan StockTick, 100), // Initialize with an appropriate buffer size
	}

	// Add the client to the clients map
	clientsMutex.Lock()
	clients[client] = true
	clientsMutex.Unlock()

	// Launch a goroutine to handle sending messages to this client
	go sendPriceToClient(client)

	// Listen for messages from the client
	for {
		_, message, err := conn.ReadMessage()
		if err != nil {
			// Remove the client if there's an error reading from the WebSocket connection
			clientsMutex.Lock()
			delete(clients, client)
			clientsMutex.Unlock()
			break
		}

		// Handle the received message
		handleWebSocketMessage(client, message)
	}
}

func handleWebSocketMessage(client *Client, message []byte) {
	// Decode the JSON message
	var msg map[string]interface{}
	err := json.Unmarshal(message, &msg)
	if err != nil {
		log.Print(err)
		return
	}

	// Handle the message based on its "opType"
	opType, ok := msg["opType"].(string)
	if !ok {
		log.Println("Missing or invalid 'opType' field in the message.")
		return
	}

	switch opType {
	case "subscribe":
		// Subscribe to symbols
		symbols, ok := msg["symbols"].([]interface{})
		if !ok {
			log.Println("Missing or invalid 'symbols' field in the message.")
			return
		}

		for _, sym := range symbols {
			symbol := sym.(string)

			// Check if symbol is already in the list
			found := false
			for _, existingSymbol := range client.symbols {
				if existingSymbol == symbol {
					found = true
					break
				}
			}

			// If symbol is not in the list, add it
			if !found {
				client.symbols = append(client.symbols, symbol)
			}
		}

		log.Printf("Client %d subscribed to symbols: %v", client.sessionID, client.symbols)
	}
}

func handleGetClients(w http.ResponseWriter, r *http.Request) {
	// Lock clients map to avoid concurrent modification
	clientsMutex.Lock()
	defer clientsMutex.Unlock()

	// Create a list of connected clients with session IDs
	clientList := make([]map[string]interface{}, 0, len(clients))
	for client := range clients {
		clientInfo := map[string]interface{}{
			"sessionID": client.sessionID,
			"symbols":   client.symbols,
			"numMsg":    len(client.messageQueue),
		}
		clientList = append(clientList, clientInfo)
	}

	// Encode the client list to JSON and send it in the response
	response, err := json.Marshal(clientList)
	if err != nil {
		http.Error(w, "Error encoding client list to JSON", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(response)
}

func handleNATSMessage(data []byte) {
	// Decode the NATS message
	parts := strings.Split(string(data), ":")
	if len(parts) != 3 {
		log.Println("Invalid message format received from NATS.")
		return
	}

	symbol, price, unixNano := parts[0], parts[1], parts[2]

	// Convert UnixNano to int64 directly
	unixNanoInt, err := strconv.ParseInt(unixNano, 10, 64)
	if err != nil {
		log.Println("Error converting UnixNano to int64:", err)
		return
	}

	// Create an internal model for the message
	stockTick := StockTick{
		Symbol:    symbol,
		Price:     price,
		Timestamp: unixNanoInt,
	}

	// Send the message to all connected WebSocket clients subscribed to that symbol concurrently
	clientsMutex.Lock()
	for client := range clients {
		if contains(client.symbols, stockTick.Symbol) {
			// Enqueue the message to the per-client message queue
			if len(client.messageQueue) == 100 {
				log.Println("Client message queue is full. Dropping message.")
				continue
			}
			client.messageQueue <- stockTick
		}
	}
	clientsMutex.Unlock()
}

func sendPriceToClient(client *Client) {
	for {
		// Dequeue a message from the per-client message queue
		stockTick := <-client.messageQueue

		// Encode the internal model to JSON
		message, err := json.Marshal(stockTick)
		if err != nil {
			log.Println("Error encoding message to JSON:", err)
			continue
		}

		// Send the message to the WebSocket client
		err = client.conn.WriteMessage(websocket.TextMessage, message)
		if err != nil {
			log.Println("Error writing message to WebSocket:", err)
			break
		}
	}
}

func contains(symbols []string, symbol string) bool {
	for _, s := range symbols {
		if s == symbol {
			return true
		}
	}
	return false
}

// Function to generate a unique session ID
func nextSessionID() int {
	sessionID++
	return sessionID
}

// Define a struct to represent the internal model of a stock tick
type StockTick struct {
	Symbol    string `json:"symbol"`
	Price     string `json:"price"`
	Timestamp int64  `json:"timestamp"` // Updated to int64
}
