package com.alladin.websockets.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.alladin.model.TickerWebsocketIncomingMessage;
import com.alladin.model.TickerWebsocketOutgoingMessage;
import com.alladin.model.TickerWebsocketSession;
import com.alladin.websockets.registry.TickerWebsocketRegistry;
import com.google.gson.Gson;

@Component
public class TickerWebSocketHandler extends TextWebSocketHandler {

	@Autowired
	TickerWebsocketRegistry tickerWebsocketRegistry;

	Gson gson = new Gson();

	@Override
	public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
		tickerWebsocketRegistry.removeTickerWebsocketSession(new TickerWebsocketSession(session, null));
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) {

		String payload = message.getPayload();

		TickerWebsocketIncomingMessage tickerWebsocketIncomingMessage = gson.fromJson(payload, 
			TickerWebsocketIncomingMessage.class);

		TickerWebsocketSession tickerWebsocketSession = new TickerWebsocketSession(session, 
			tickerWebsocketIncomingMessage.getSymbols());
		
		if(tickerWebsocketIncomingMessage.getOpType().equals("subscribe")) {
			tickerWebsocketRegistry.addTickerWebsocketSession(tickerWebsocketSession);
		}

		
	}

	public void sendTick(TickerWebsocketOutgoingMessage tick) {
		tickerWebsocketRegistry.get(tick.getSymbol()).forEach(tickerWebsocketSession -> {
			try {
				tickerWebsocketSession.getSession().sendMessage(new TextMessage(tick.toString()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

}