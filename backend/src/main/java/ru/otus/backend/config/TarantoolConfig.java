package ru.otus.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tarantool.TarantoolClient;
import org.tarantool.TarantoolClientConfig;
import org.tarantool.TarantoolClientImpl;
import org.tarantool.TarantoolClusterClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

@Configuration
public class TarantoolConfig {

    @Value("${spring.tarantool.host}")
    private String host;

    @Value("${spring.tarantool.port}")
    private int port;

    @Value("${spring.tarantool.username}")
    private String username;

    @Value("${spring.tarantool.password}")
    private String password;

    @Value("${spring.tarantool.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${spring.tarantool.socket-timeout:5000}")
    private int socketTimeout;

    @Bean
    public TarantoolClient tarantoolClient() {
        TarantoolClientConfig config = new TarantoolClientConfig();
        config.username = username;
        config.password = password;
        config.connectionTimeout = connectionTimeout;

        return new TarantoolClientImpl(host + ":" + port, config);
    }
}