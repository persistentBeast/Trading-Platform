package com.vertx.tickr.core.verticles;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebsocketServerVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {

        log.info("Starting verticle on thread: " + Thread.currentThread().getName());


        Router router = Router.router(vertx);

        router.route("/alladin/ticks").handler(routingContext -> {
            routingContext.request().toWebSocket().onComplete(
                    (res) -> {
                        String id = UUID.randomUUID().toString();
                        log.info("WebSocket connected, id: " + id);
                        vertx.deployVerticle(new WebsocketClientVerticle(res, new ArrayList<>(), id));
                        webSocketHandler2(res, id);
                    }, 
                    (err) -> {
                        log.error("WebSocket failed", err);
                    }
                );
        });

        vertx.createHttpServer().requestHandler(router).listen(8080);
    }

    private void webSocketHandler2(ServerWebSocket ws, String id) {

        ws.textMessageHandler(message -> {
            log.info("Received message: " + message);
            JsonObject json = new JsonObject(message);
            json.put("id", id);
            vertx.eventBus().publish("event.wsc.lifecycle", json);
            ws.writeTextMessage("Echo: " + "Ack");
        });
    }

    
}