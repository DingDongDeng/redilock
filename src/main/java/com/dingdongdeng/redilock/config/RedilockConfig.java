package com.dingdongdeng.redilock.config;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedilockConfig {

    private final String REDIS_SCHEME_PROTOCOL = "redis://";
    private final String url;
    private final Long connectionTimeout;
    private final Long readTimeout;

    public RedilockConfig(
        @Value("${redilock.redis.url}") String url,
        @Value("${redilock.redis.connectionTimeout}") String connectionTimeout,
        @Value("${redilock.redis.readTimeout}") String readTimeout) {

        this.url = url;
        this.connectionTimeout = Long.parseLong(Objects.isNull(connectionTimeout) ? "1000" : connectionTimeout);
        this.readTimeout = Long.parseLong(Objects.isNull(readTimeout) ? "1000" : connectionTimeout);

    }

    @Bean("redilockRedissonClient")
    public RedissonClient redilockRedissonClient() {
        String address = StringUtils.indexOf(this.url, REDIS_SCHEME_PROTOCOL) == 0 ? this.url : REDIS_SCHEME_PROTOCOL + this.url;

        Config config = new Config();
        config.useSingleServer()
            .setAddress(address)
            .setConnectTimeout(Objects.nonNull(this.connectionTimeout) ? this.connectionTimeout.intValue() : 10000) //default 10000
        ;
        return Redisson.create(config);
    }

}
