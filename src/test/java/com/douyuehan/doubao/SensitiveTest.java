package com.douyuehan.doubao;

import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * 敏感词测试类
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/4/11 10:31
 **/
public class SensitiveTest {


    @Test
    public void generateText() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        // 生成9995个正常字符
        for (int i = 0; i < 9995; i++) {
            char c = (char) (random.nextInt(20902) + 19968);  // 随机生成中文字符
            sb.append(c);
        }

        // 在随机位置插入5个"嫖娼"
        for (int i = 0; i < 5; i++) {
            int position = random.nextInt(sb.length());
            sb.insert(position, "赌博吸毒吸毒吸毒吸毒吸毒吸毒吸毒吸毒吸毒吸毒");
        }

        String text = sb.toString();
        System.out.println(text);
    }

}
