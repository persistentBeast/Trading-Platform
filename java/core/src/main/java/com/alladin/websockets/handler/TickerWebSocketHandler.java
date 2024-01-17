package com.alladin.websockets.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;
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

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TickerWebSocketHandler extends TextWebSocketHandler {

	@Autowired
	TickerWebsocketRegistry tickerWebsocketRegistry;

	Gson gson = new Gson();

	ExecutorService executorService = new ThreadPoolExecutor(50, 100, 1, TimeUnit.MINUTES, 
		new ArrayBlockingQueue<>(10000));

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.info("New session established: {}", session.getId());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
		log.info("Session closed: {}", session.getId());
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
		if(tickerWebsocketRegistry.get(tick.getSymbol()) == null) {
			return;
		}
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		tickerWebsocketRegistry.get(tick.getSymbol()).forEach(tickerWebsocketSession -> {

			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					if(!tickerWebsocketRegistry.getTickerWebsocketSessions().containsKey(tickerWebsocketSession.getSession().getId())){
						return;
					}				
					tickerWebsocketSession.getSession().sendMessage(new TextMessage(gson.toJson(tick)));
				} catch (Exception e) {
					e.printStackTrace();
					log.error("Error sending tick to session: {}, err :{}", tickerWebsocketSession.getSession().getId(), e.getMessage());
				}
			}, executorService);

			futures.add(future);
		});

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();

	}

}