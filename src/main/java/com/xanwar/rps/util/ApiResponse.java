package com.xanwar.rps.util;

import java.util.HashMap;
import java.util.Map;

public final class ApiResponse {

    private ApiResponse() {}

    public static Map<String, Object> success() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        return body;
    }

    public static Map<String, Object> success(String messageKey, Object messageValue) {
        Map<String, Object> body = success();
        body.put(messageKey, messageValue);
        return body;
    }

    public static Map<String, Object> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", message);
        return body;
    }
}
