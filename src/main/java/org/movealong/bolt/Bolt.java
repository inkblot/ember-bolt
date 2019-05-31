package org.movealong.bolt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.jnape.palatable.lambda.io.IO;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.Map;

import static com.jnape.palatable.lambda.adt.Try.trying;
import static com.jnape.palatable.lambda.adt.Try.withResources;
import static com.jnape.palatable.lambda.functions.Effect.effect;
import static com.jnape.palatable.lambda.functions.builtin.fn2.Alter.alter;
import static com.jnape.palatable.lambda.functions.specialized.Kleisli.kleisli;
import static com.jnape.palatable.lambda.io.IO.io;
import static java.util.Collections.singletonMap;

@Slf4j
public class Bolt implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final IO<APIGatewayProxyResponseEvent> handle;

    public Bolt() {
        Map<String, String> environment = System.getenv();
        String appName = environment.get("APP_NAME");
        String redisServer = environment.get("REDIS_SERVER");
        handle = withResources(() -> new Jedis(redisServer),
                               jedis -> trying(() -> {
                                   jedis.connect();
                                   return jedis.get(appName + ":index:current-content");
                               }))
                .toEither()
                .peek(ex -> io(() -> log.error("Failed to serve request", ex)),
                      __ -> io(() -> log.info("Served current index")))
                .match(ex -> responseEvent(500, ex.getMessage(), singletonMap("Content-Type", "text/plain")),
                       idx -> responseEvent(200, idx, singletonMap("Content-Type", "text/html")));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        log.info("Getting index");
        return handle.unsafePerformIO();
    }

    private static IO<APIGatewayProxyResponseEvent> responseEvent(int statusCode, String message, Map<String, String> headers) {
        return kleisli(alter(effect((APIGatewayProxyResponseEvent r) -> io(() -> r.setHeaders(headers)))))
                .andThen(kleisli(alter(effect(r -> io(() -> r.setStatusCode(statusCode))))))
                .andThen(kleisli(alter(effect(r -> io(() -> r.setBody(message))))))
                .apply(new APIGatewayProxyResponseEvent());
    }
}
