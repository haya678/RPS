package com.xanwar.rps.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionUserResolver {

    public String resolveTornId(HttpSession session, String bodyTornId) {
        Object sessionTornId = session.getAttribute(SessionKeys.TORN_ID);
        if (sessionTornId instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new IllegalArgumentException("Not authenticated. Log in with your Torn API key.");
    }
}
