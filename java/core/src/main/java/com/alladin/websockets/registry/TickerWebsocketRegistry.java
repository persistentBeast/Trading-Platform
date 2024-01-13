package com.alladin.websockets.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.context.annotation.Configuration;

import com.alladin.model.TickerWebsocketSession;

import lombok.Getter;

@Configuration
@Getter
public class TickerWebsocketRegistry {

    private ConcurrentHashMap<String, TickerWebsocketSession> tickerWebsocketSessions = new ConcurrentHashMap<>();
    
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<TickerWebsocketSession>> tickerWebsocketSessionsBySymbol = new ConcurrentHashMap<>();


    public void addTickerWebsocketSession(TickerWebsocketSession tickerWebsocketSession) {
        if(tickerWebsocketSessions.containsKey(tickerWebsocketSession.getSession().getId())) {
            return;
        }
        tickerWebsocketSessions.put(tickerWebsocketSession.getSession().getId(), tickerWebsocketSession);
        tickerWebsocketSession.getSymbols().forEach(symbol -> {
            tickerWebsocketSessionsBySymbol.putIfAbsent(symbol, new ConcurrentLinkedQueue<>());
            tickerWebsocketSessionsBySymbol.get(symbol).add(tickerWebsocketSession);
        });
    }

    public void removeTickerWebsocketSession(TickerWebsocketSession tickerWebsocketSession) {
        tickerWebsocketSessions.remove(tickerWebsocketSession.getSession().getId());
    }

    public ConcurrentLinkedQueue<TickerWebsocketSession> get(String symbol) {
        return tickerWebsocketSessionsBySymbol.get(symbol);
    }

    
}
