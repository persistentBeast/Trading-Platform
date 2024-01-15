package com.alladin.util;

import java.nio.charset.StandardCharsets;

import com.alladin.model.TickerWebsocketOutgoingMessage;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TicksUtil {

    public static TickerWebsocketOutgoingMessage parseTickFromRawBytes(byte[] rawBytes) {
        String tick = new String(rawBytes, StandardCharsets.UTF_8);
        String[] tickParts = tick.split(":");
        return new TickerWebsocketOutgoingMessage(tickParts[0], tickParts[1], tickParts[2]);
    } 
    
}
