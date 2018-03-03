package org.movealong.bolt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import redis.clients.jedis.Jedis;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

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
            return response;
        } catch (Throwable e) {
            response.setStatusCode(500);
            response.setBody(String.format("%s:\n%s", e.getMessage(), getStackTrace(e)));
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
