package com.douyuehan.doubao.config;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redission 配置类
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/4/19 18:42
 **/

@Configuration
public class RedissonConfig {

    @Autowired
    private RedisConfig redisConfig;

    @Bean
    public Redisson redisson() {
        Config config = new Config();
        // 单机
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(11);
        return (Redisson) Redisson.create(config);
    }
}
