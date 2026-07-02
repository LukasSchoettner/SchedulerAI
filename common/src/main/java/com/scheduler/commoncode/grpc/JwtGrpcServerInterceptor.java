package com.scheduler.commoncode.grpc;

import com.scheduler.commoncode.security.JwtUtil;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@GrpcGlobalServerInterceptor
@Component
public class JwtGrpcServerInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtGrpcServerInterceptor.class);
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Autowired
    private JwtUtil jwtUtil;

    public static final Context.Key<Long> CUSTOMER_ID_CTX_KEY = Context.key("customerId");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        log.debug("Incoming call: {}", call.getMethodDescriptor().getFullMethodName());
        String authHeader = headers.get(AUTHORIZATION_KEY);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error(authHeader == null
                    ? "Missing Authorization header"
                    : "Authorization header does not start with Bearer");
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"), headers);
            return new ServerCall.Listener<>() {};
        }

        String token = authHeader.substring(7).trim();
        log.debug("Extracted JWT: {}", token);

        try {
            Claims claims = jwtUtil.getClaims(token);

            // 🔹 Try to extract "id" claim, but don't die if it's missing
            Long customerId = null;
            Object idClaim = claims.get("id");
            if (idClaim instanceof Integer i) {
                customerId = i.longValue();
            } else if (idClaim instanceof Long l) {
                customerId = l;
            }

            if (customerId != null) {
                log.debug("JWT validated; customerId={}", customerId);
                Context ctx = Context.current().withValue(CUSTOMER_ID_CTX_KEY, customerId);
                return Contexts.interceptCall(ctx, call, headers, next);
            } else {
                log.debug("JWT validated; no 'id' claim present – proceeding without customerId in context.");
                return Contexts.interceptCall(Context.current(), call, headers, next);
            }

        } catch (ExpiredJwtException eje) {
            log.error("JWT expired: {}", eje.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Expired JWT"), headers);

        } catch (SignatureException se) {
            log.error("JWT signature invalid: {}", se.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT signature"), headers);

        } catch (MalformedJwtException mje) {
            log.error("JWT malformed: {}", mje.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Malformed JWT"), headers);

        } catch (Exception e) {
            log.error("JWT parsing error: {}", e.getMessage(), e);
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT"), headers);
        }

        return new ServerCall.Listener<>() {};
    }
}

