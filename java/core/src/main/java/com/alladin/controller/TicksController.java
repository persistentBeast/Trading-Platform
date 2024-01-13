package com.alladin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.TextMessage;

import com.alladin.model.TickerWebsocketOutgoingMessage;
import com.alladin.model.TickerWebsocketSession;
import com.alladin.websockets.registry.TickerWebsocketRegistry;
import com.google.gson.Gson;

@RestController
public class TicksController {


    @Autowired
	TickerWebsocketRegistry tickerWebsocketRegistry;

    Gson gson = new Gson();


    //Return ticks registry details, all websocket sessions and their subscribed symbols
    @GetMapping("/current-ticks-sessions")
    public Map<String, List<String>> getSessions() {
        Map<String, List<String>> sessions = new HashMap<>();
        for (TickerWebsocketSession session : tickerWebsocketRegistry.getTickerWebsocketSessions().values()) {
            sessions.put(session.getSession().getId(), session.getSymbols());
        }
        return sessions;
    }
    
    @PostMapping("/send-ticks")
    public void sendTicks(@RequestBody TickerWebsocketOutgoingMessage msg) {

        if(msg.getSymbol() == null || msg.getSymbol().isEmpty()) {
            return;
        }

        if(tickerWebsocketRegistry.get(msg.getSymbol()) == null) {
            return;
        }

        tickerWebsocketRegistry.get(msg.getSymbol()).forEach(tickerWebsocketSession -> {
			try {
                if(!tickerWebsocketRegistry.getTickerWebsocketSessions().containsKey(tickerWebsocketSession.getSession().getId())){
                    return;
                }
				tickerWebsocketSession.getSession().sendMessage(new TextMessage(gson.toJson(msg)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
    }


}
