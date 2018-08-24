package org.movealong.bolt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.Map;

import static java.util.Collections.singletonMap;

@Slf4j
public class Bolt implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final String appName;
    private final String redisServer;

    public Bolt() {
        Map<String, String> environment = System.getenv();
        appName = environment.get("APP_NAME");
        redisServer = environment.get("REDIS_SERVER");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
                                                      Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            response.setStatusCode(200);
            response.setBody(getIndex());
            response.setHeaders(singletonMap("Content-Type", "text/html"));
            log.info("Served current index");
            return response;
        } catch (Throwable e) {
            log.error("Failed to serve request", e);
            response.setStatusCode(500);
            response.setBody(e.getMessage());
            response.setHeaders(singletonMap("Content-Type", "text/plain"));
        }
        return response;
    }

    private String getIndex() {
        String index;
        try (Jedis jedis = new Jedis(redisServer)) {
            jedis.connect();
            index = jedis.get(appName + ":index:current-content");
        }
        return index;
    }
}
