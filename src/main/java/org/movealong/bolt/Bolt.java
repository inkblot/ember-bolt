package org.movealong.bolt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.jnape.palatable.lambda.adt.Either;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.io.IO;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.util.Map;

import static com.jnape.palatable.lambda.functions.Effect.effect;
import static com.jnape.palatable.lambda.functions.builtin.fn2.Alter.alter;
import static com.jnape.palatable.lambda.functions.builtin.fn2.AutoBracket.autoBracket;
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
        int redisPort = Integer.parseInt(environment.get("REDIS_PORT"));
        HostAndPort redisEndpoint = new HostAndPort(redisServer, redisPort);

        this.handle = io(() -> log.info("Getting index"))
                .discardL(fetchCurrentIndex(appName, redisEndpoint))
                .flatMap(e -> e.match(errorResonse(), successResponse()));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        return handle.unsafePerformIO();
    }

    private static IO<Either<Throwable, String>> fetchCurrentIndex(String appName, HostAndPort redisEndpoint) {
        return autoBracket(io(() -> new Jedis(redisEndpoint)).flatMap(alter(j -> io(j::connect))),
                           jedis -> io(() -> jedis.get(appName + ":index:current-content"))).safe();
    }

    private static Fn1<Throwable, IO<APIGatewayProxyResponseEvent>> errorResonse() {
        return ex -> io(() -> log.error("Failed to serve request", ex))
                .discardL(responseEvent(500, ex.getMessage(), singletonMap("Content-Type", "text/plain")));
    }

    private static Fn1<String, IO<APIGatewayProxyResponseEvent>> successResponse() {
        return idx -> io(() -> log.info("Served current index"))
                .discardL(responseEvent(200, idx, singletonMap("Content-Type", "text/html")));
    }

    private static IO<APIGatewayProxyResponseEvent> responseEvent(int statusCode, String message, Map<String, String> headers) {
        return kleisli(alter(effect((APIGatewayProxyResponseEvent r) -> io(() -> r.setHeaders(headers)))))
                .andThen(kleisli(alter(effect(r -> io(() -> r.setStatusCode(statusCode))))))
                .andThen(kleisli(alter(effect(r -> io(() -> r.setBody(message))))))
                .apply(new APIGatewayProxyResponseEvent());
    }
}
