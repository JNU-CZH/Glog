package com.douyuehan.doubao.utils;

import io.rebloom.client.Client;
import org.springframework.stereotype.Component;

/**
 * 布隆过滤器
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/4/14 00:14
 **/
@Component
public class BloomFilterUtil {

    private Client client;

    public BloomFilterUtil() {
        // 构造方法保持空
    }

    private void initBloomFilter() {
        if (client == null) {
            client = new Client("127.0.0.1", 6379);
            client.createFilter("filter", 10000, 0.01);
        }
    }

    public void addItem(String postId) {
        initBloomFilter();
        client.add("filter", postId);
    }

    public boolean isIn(String postId) {
        initBloomFilter();
        return client.exists("filter", postId);
    }
}
