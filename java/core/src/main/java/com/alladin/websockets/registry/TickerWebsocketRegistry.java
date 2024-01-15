package com.alladin.websockets.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Configuration;

import com.alladin.model.TickerWebsocketSession;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Getter
@Slf4j
public class TickerWebsocketRegistry {

    private ConcurrentHashMap<String, TickerWebsocketSession> tickerWebsocketSessions = new ConcurrentHashMap<>();
    
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<TickerWebsocketSession>> tickerWebsocketSessionsBySymbol = new ConcurrentHashMap<>();


    public void addTickerWebsocketSession(TickerWebsocketSession tickerWebsocketSession) {

        List<String> newSymbols = new ArrayList<>();

        if(tickerWebsocketSessions.containsKey(tickerWebsocketSession.getSession().getId())) {
            newSymbols = tickerWebsocketSession.getSymbols()
                .stream()
                .filter(symbol -> !tickerWebsocketSessions.get(tickerWebsocketSession.getSession().getId()).getSymbols().contains(symbol))
                .collect(Collectors.toList());
            
            tickerWebsocketSessions.get(tickerWebsocketSession.getSession().getId()).getSymbols().addAll(newSymbols);
            log.info("Session already exists, updating symbols, new symbols: {}", 
                tickerWebsocketSessions.get(tickerWebsocketSession.getSession().getId()).getSymbols());

        }else{
            tickerWebsocketSessions.put(tickerWebsocketSession.getSession().getId(), tickerWebsocketSession);
            newSymbols = tickerWebsocketSession.getSymbols();
            log.info("New session added, symbols: {}", newSymbols);
        }
        
        newSymbols.forEach(symbol -> {
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
