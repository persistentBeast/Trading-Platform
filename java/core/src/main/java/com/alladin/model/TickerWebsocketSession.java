package com.alladin.model;

import java.util.List;

import org.springframework.web.socket.WebSocketSession;

public class TickerWebsocketSession {
    
    private WebSocketSession session;
    private List<String> symbols;

    public TickerWebsocketSession(WebSocketSession session, List<String> symbols) {
        this.session = session;
        this.symbols = symbols;
    }

    public WebSocketSession getSession() {
        return session;
    }
    
    public List<String> getSymbols() {
        return symbols;
    }

}
