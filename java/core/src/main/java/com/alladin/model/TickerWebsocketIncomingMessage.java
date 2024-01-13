package com.alladin.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TickerWebsocketIncomingMessage {

    private String opType;
    private List<String> symbols;

    
}