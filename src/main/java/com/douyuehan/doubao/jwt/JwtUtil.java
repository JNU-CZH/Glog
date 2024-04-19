package com.douyuehan.doubao.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    public static final long EXPIRATION_TIME = 3600_000_0L; // 10 hour

    // 设置access_token时间为12小时
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 43200000; // 12 hour in milliseconds

    // 设置access_token时间为7天
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 604800000; // 7 days in milliseconds
    public static final String SECRET = "ThisIsASecret";//please change to your own encryption secret.
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String USER_NAME = "userName";

    public static String generateToken(String userId, long expireTime) {
        HashMap<String, Object> map = new HashMap<>();
        //you can put any data in the map
        map.put(USER_NAME, userId);
        String jwt = Jwts.builder()
                // payload载荷，可以设置多个值
                .setClaims(map)
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
        return jwt; //jwt前面一般都会加Bearer
    }

    public static String generateAccessToken(String username) {
        return generateToken(username, ACCESS_TOKEN_EXPIRATION_TIME);
    }

    public static String generateRefreshToken(String username) {
        return generateToken(username, REFRESH_TOKEN_EXPIRATION_TIME);
    }

    public static boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    public static String extractUsername(String token) {
        return getClaims(token).getSubject();
    }


    private static Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }

    public static HttpServletRequest validateTokenAndAddUserIdToHeader(HttpServletRequest request) {
        String token = request.getHeader(HEADER_STRING);
        if (token != null) {
            // parse the token.
            try {
                Map<String, Object> body = Jwts.parser()
                        .setSigningKey(SECRET)
                        .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                        .getBody();
                return new CustomHttpServletRequest(request, body);
            } catch (Exception e) {
                logger.info(e.getMessage());
                throw new TokenValidationException(e.getMessage());
            }
        } else {
            throw new TokenValidationException("Missing token");
        }
    }

    // 自定义HttpServletRequest类，继承自HttpServletRequestWrapper
    public static class CustomHttpServletRequest extends HttpServletRequestWrapper {
        // 用于存储JWT载荷中的声明
        private Map<String, String> claims;

        // 构造函数，接收一个HttpServletRequest对象和一个Map对象，Map对象包含JWT载荷中的声明
        public CustomHttpServletRequest(HttpServletRequest request, Map<String, ?> claims) {
            super(request); // 调用父类的构造函数
            this.claims = new HashMap<>();
            // 将Map对象中的声明转换为字符串，并存储在claims中
            claims.forEach((k, v) -> this.claims.put(k, String.valueOf(v)));
        }

        // 重写getHeaders方法
        @Override
        public Enumeration<String> getHeaders(String name) {
            // 如果claims中包含指定的声明，那么返回这个声明的值
            if (claims != null && claims.containsKey(name)) {
                return Collections.enumeration(Arrays.asList(claims.get(name)));
            }
            // 否则，调用父类的getHeaders方法
            return super.getHeaders(name);
        }

    }

    static class TokenValidationException extends RuntimeException {
        public TokenValidationException(String msg) {
            super(msg);
        }
    }
}