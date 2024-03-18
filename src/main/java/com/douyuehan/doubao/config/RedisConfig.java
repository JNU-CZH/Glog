package com.douyuehan.doubao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis配置文件
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/3/18 12:40
 **/
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 设置key的序列化方法
        template.setKeySerializer(RedisSerializer.string());
        // 设置普通value的序列化方法
        template.setValueSerializer(RedisSerializer.json());
        // 设置hash的key的序列化方法
        template.setHashKeySerializer(RedisSerializer.string());
        // 设置hash的value的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        template.afterPropertiesSet();

        return template;

    }

}
