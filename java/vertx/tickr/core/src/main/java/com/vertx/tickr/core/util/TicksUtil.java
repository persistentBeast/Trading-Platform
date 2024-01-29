package com.vertx.tickr.core.util;

import java.nio.charset.StandardCharsets;

import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TicksUtil {

    public static JsonObject parseTickFromRawBytes(byte[] rawBytes) {
        String tick = new String(rawBytes, StandardCharsets.UTF_8);
        String[] tickParts = tick.split(":");
        return new JsonObject().put("symbol", tickParts[0]).put("price", tickParts[1]).put("timestamp", tickParts[2]);
    } 
    
}
