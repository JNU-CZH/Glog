package com.douyuehan.doubao.model.dto;

/**
 * token刷新请求类
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/4/13 23:04
 **/
public class TokenRequestDTO {

    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
