package com.douyuehan.doubao;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
/**
 * 查询性能测试
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/4/11 15:14
 **/
public class ESAndMySQLTest {

    @Test
    public void query() {
        String url = "jdbc:mysql://localhost:3306/doubao";
        String user = "root";
        String password = "123456";
        String query = "SELECT * FROM bms_post_copy1 WHERE content LIKE ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "%people%");  // 设置模糊查询的关键字

            long startTime = System.nanoTime();
            ResultSet rs = stmt.executeQuery();
            long endTime = System.nanoTime();

            long duration = (endTime - startTime) / 1_000_000;  // 转换为毫秒
            System.out.println("Query duration: " + duration + " ms");

            // 处理结果集...
            while (rs.next()) {
                // ...
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
