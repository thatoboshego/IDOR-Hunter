package com.idorhunter.util;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Records the live server port so in-process tooling can call the app over real HTTP. */
@Component
public class ServerPortCapture {

    private volatile int port = -1;

    @EventListener
    public void onServerReady(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
    }

    public int getPort() {
        return port;
    }
}
