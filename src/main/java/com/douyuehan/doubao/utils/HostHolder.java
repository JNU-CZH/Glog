package com.douyuehan.doubao.utils;

/**
 * @
 * @Author: ChenZhiHui
 * @DateTime: 2024/3/18 12:42
 **/

import com.douyuehan.doubao.model.entity.UmsUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 持有用户的信息，用户代替session对象
 * 主要包括用户设置和用户获取以及用户清除三部分
 *
 * @Author: ChenZhiHui
 * @DateTime: 2023/6/2 00:33
 **/
@Component
public class HostHolder{

    private ThreadLocal<String> user = new ThreadLocal<>();

    public void setUser(String userId) {
        user.set(userId);
    }

    public String getUser() {
        return user.get();
    }

    public void clear() {
        user.remove();
    }
}
