package com.douyuehan.doubao.model.dto;

/**
 * token刷新返回类
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/4/13 23:06
 **/
public class TokenResponse {
    private String accessToken;

    public TokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
