package com.example.grpc.config;

import com.example.grpc.interceptor.GrpcInterceptor;
import com.example.grpc.service.GrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GrpcServerConfig {

    @Value("${grpc.port:8888}")
    Integer grpcPort;

    private final GrpcInterceptor handler;
    private final GrpcService rpcService;

    // Grpc Java는 Netty 기반 http2.0 프로토콜을 사용
    @Bean
    public Server grpcServer() {
        return ServerBuilder
                .forPort(grpcPort)
                .addService(rpcService)
                .intercept(handler)
                .build();
    }
}
