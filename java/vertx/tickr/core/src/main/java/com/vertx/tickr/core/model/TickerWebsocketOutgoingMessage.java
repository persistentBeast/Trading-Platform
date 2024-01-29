package com.vertx.tickr.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class TickerWebsocketOutgoingMessage {

    private String symbol;
    private String price;
    private String timestamp;

}
