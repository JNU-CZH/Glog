package com.douyuehan.doubao.utils;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * DFA敏感词过滤类
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/4/11 10:11
 **/

@Component
public class DFASensitiveFilter {
    // 敏感词树
    private Map<Character, Map> sensitiveWordsMap = new HashMap<>();

    // 在项目启动的时候就构建好敏感词树
    @PostConstruct
    public void init() {
        // 将敏感词过滤的文件读入
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("sensitive-word.txt");
        // 采用缓冲池的方式进行读入
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addSensitiveWord(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 添加敏感词到敏感词树
    public void addSensitiveWord(String word) {
        Map<Character, Map> currentMap = sensitiveWordsMap;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Map<Character, Map> subMap = currentMap.get(c);
            if (subMap == null) {
                subMap = new HashMap<>();
                currentMap.put(c, subMap);
            }
            currentMap = subMap;
        }
        // 标记敏感词的结尾
        currentMap.put('\0', null);
    }

    // 过滤敏感词
    public String filter(String text) {
        StringBuilder result = new StringBuilder();
        int length = text.length();
        int start = 0;
        int position = 0;

        while (position < length) {
            char c = text.charAt(position);
            Map<Character, Map> currentMap = sensitiveWordsMap.get(c);
            if (currentMap != null) {
                Map<Character, Map> subMap = currentMap;
                int matchLength = 1;
                int endPosition = position;
                while (++position < length && (subMap = subMap.get(text.charAt(position))) != null) {
                    matchLength++;
                    if (subMap.containsKey('\0')) {
                        // 匹配到了敏感词，但不立即替换，记录下当前位置
                        endPosition = position;
                    }
                }
                if (endPosition > start) {
                    // 找到了更长的敏感词，进行替换
                    result.append(replaceMask(text.substring(start, endPosition + 1)));
                    start = endPosition + 1;
                } else {
                    // 没有找到更长的敏感词，将当前字符添加到结果中
                    result.append(c);
                    start++;
                    position = start;
                }
            } else {
                // 如果当前字符不是敏感词的开头，则将其添加到结果中
                result.append(c);
                start++;
                position = start;
            }
        }
        // 将剩余部分添加到结果中
        result.append(text.substring(start));
        return result.toString();
    }

    // 替换敏感词为*
    private String replaceMask(String sensitiveWord) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sensitiveWord.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }

}