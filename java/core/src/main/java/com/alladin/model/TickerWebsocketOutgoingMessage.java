package com.alladin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TickerWebsocketOutgoingMessage {

    private String symbol;
    private String price;
    private String timestamp;
    
}
