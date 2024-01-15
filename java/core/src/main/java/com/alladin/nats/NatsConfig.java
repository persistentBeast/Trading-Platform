package com.alladin.nats;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alladin.model.TickerWebsocketOutgoingMessage;
import com.alladin.util.TicksUtil;
import com.alladin.websockets.handler.TickerWebSocketHandler;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

@Configuration
public class NatsConfig {

    @Autowired
    TickerWebSocketHandler tickerWebSocketHandler;

    @Bean("natsConnection")
    public Connection getConnection() {

        try {
            Connection nc = Nats.connect("nats://localhost:4222");


            Dispatcher d = nc.createDispatcher((msg) -> {
                TickerWebsocketOutgoingMessage payload = TicksUtil.parseTickFromRawBytes(msg.getData());
                tickerWebSocketHandler.sendTick(payload);
            });

            d.subscribe( "stock.ticks.*");

            return nc;
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    
}
