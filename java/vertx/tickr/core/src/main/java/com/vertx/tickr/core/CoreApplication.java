package com.vertx.tickr.core;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vertx.tickr.core.model.TickerWebsocketOutgoingMessage;
import com.vertx.tickr.core.util.TicksUtil;
import com.vertx.tickr.core.verticles.WebsocketServerVerticle;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@SpringBootApplication
public class CoreApplication {

	static Vertx vertx = Vertx.vertx();

	public static void main(String[] args) {
		SpringApplication.run(CoreApplication.class, args);

		 try {
            Connection nc = Nats.connect("nats://localhost:4222");


            Dispatcher d = nc.createDispatcher((msg) -> {
				JsonObject json = TicksUtil.parseTickFromRawBytes(msg.getData());
                vertx.eventBus().publish("event.stock.tick", json);
            });

            d.subscribe( "stock.ticks.*");

        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


		vertx.deployVerticle(new WebsocketServerVerticle());

	}

}
