package com.scheduler.commoncode.grpc;

import io.grpc.*;
import org.springframework.beans.factory.annotation.Autowired;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.stereotype.Component;

@GrpcGlobalClientInterceptor
@Component
public class JwtGrpcClientInterceptor implements ClientInterceptor {

    @Autowired
    private JwtTokenProvider jwtTokenProvider; // defined below

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Remove any existing Authorization header first
                Metadata.Key<String> authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
                headers.removeAll(authKey);

                // Inject internal token
                String jwt = jwtTokenProvider.getToken();
                headers.put(authKey, "Bearer " + jwt);

                System.out.println("Injecting internal JWT: " + jwt); // for debugging

                super.start(responseListener, headers);
            }
        };
    }
}

