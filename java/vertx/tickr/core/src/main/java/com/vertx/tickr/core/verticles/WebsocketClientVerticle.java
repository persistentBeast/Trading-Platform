package com.vertx.tickr.core.verticles;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebsocketClientVerticle extends AbstractVerticle {

    ServerWebSocket ws;

    List<String> symbols = new ArrayList<>();

    String id;

    public WebsocketClientVerticle(ServerWebSocket ws2, List jsonArray, String id) {
        //TODO Auto-generated constructor stub
        this.ws = ws2;
        this.symbols = (List<String>) symbols;
        this.id = id;
    }

    @Override
    public void start() throws Exception {

        System.out.println("Starting verticle on thread: " + Thread.currentThread().getName());

        vertx.eventBus().consumer("event.wsc.lifecycle", new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject json = message.body();
                JsonArray symbols = json.getJsonArray("symbols");
                if (json.getString("id").equals(id)) {
                    for (Object symbol : symbols) {
                        if (!WebsocketClientVerticle.this.symbols.contains(symbol)) {
                            WebsocketClientVerticle.this.symbols.add((String) symbol);
                        }
                    }
                }
                // log.info("WsId : {}, Symbols: {}", id, symbols);
                
            }
        });

        vertx.eventBus().consumer("event.stock.tick", new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                // log.info("Sending tick: " + message.body());
                JsonObject json = message.body();
                String symbol = json.getString("symbol");
                if (symbols.contains(symbol)) {
                    ws.writeTextMessage(json.toString());
                }
            }
        });
        

    }

    
}