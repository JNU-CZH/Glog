package com.douyuehan.doubao.utils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.commons.lang3.CharUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词过滤类
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/4/11 09:27
 **/
@Component
public class TrieSensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(TrieSensitiveFilter.class);

    // 设置需要替换符号的内容
    private static final String REPLACEMENT = "***";

    // 根节点
    private TrieNode rootNode = new TrieNode();


    //1、定义一个Trie内部类
    private class TrieNode {

        // 关键词结束标识：默认为false
        private boolean isKeywordEnd = false;

        // 子节点，因为每个节点都可能有很多个子节点，所以使用map来存储
        Map<Character, TrieNode> subNodes =  new HashMap<>();

        // 添加子节点：现在操作的地方是当前节点，subNodes是它的子节点集合，直接往里添加就行
        private void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        // 获取子节点
        public TrieNode getSubNode(Character c) {
            TrieNode trieNode = subNodes.get(c);
            return  trieNode;
        }

        // 判断是不是关键词标识符
        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }
    }


    // 2、我们要在项目启动的时候就构建好前缀树 -> @PostConstruct注解
    @PostConstruct
    public void init() throws IOException {
        // 将敏感词过滤的文件读入
        // this.getClass().getClassLoader() 这一步是获得target的文件
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("sensitive-word.txt");
        // 采用缓冲池的方式进行读入
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        // 定义接收每个敏感词的字符串
        String keyword;
        while( (keyword = reader.readLine()) != null) {
            this.addKeyword(keyword); // 将字符串keyword构造成前缀树
        }
    }

    // 3、实现字符串构造前缀树操作
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for(int i = 0; i < keyword.length(); i++){
            char c = keyword.charAt(i);
            // 判断当前c是否已经有存储
            TrieNode subNode = tempNode.getSubNode(c);
            if(subNode == null) {
                // 新建一个节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }
            // 当前节点指向子节点，开始下一伦循环
            tempNode = subNode;

            // 设置结束标识
            if(i == keyword.length() - 1){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    // 4、实现敏感词过滤
    public String filter(String text) {
        if(StringUtils.isBlank(text)) {
            return null;
        }
        // 定义三个指针：1指向前缀树根节点、2和3指向text起始位置，2的作用定位初始位置，3找到末尾位置
        TrieNode point1 = rootNode;
        int begin = 0;
        int position = 0;

        // 设置返回结果
        StringBuilder sb = new StringBuilder();

        while(position < text.length()) {
            char c = text.charAt(position); // 这样可以获得需要判断的字符
            // 判断c是不是特殊字符
            if(isSymbol(c)) {
                // 若point1位于根节点的话，将符号计入结果，begin往后走
                if(point1 == rootNode) {
                    sb.append(c);
                    begin++;
                }
                position++;
                continue;
            }

            // 这个时候c不是特殊字符 -> 因为point1刚开始的时候指向的是没有值的根节点
            point1 = point1.getSubNode(c);
            if(point1 == null){
                // 说明这不是敏感词
                sb.append(text.charAt(begin));
                position = ++begin;
                point1 = rootNode;
            } else if (point1.isKeywordEnd()) {
                // 发现敏感词
                sb.append(REPLACEMENT);
                begin = ++position;
                point1 = rootNode;
            } else {
                // 检查下一个字符
                position++;
            }
        }
        // 将最后一批字符加入到结果中
        sb.append(text.substring(begin));
        return sb.toString();

    }

    // 5、判断是不是特殊字符
    public boolean isSymbol(Character c) {

        if(CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF) ){
            // 说明是特殊字符
            return true;
        }
        return false;
    }


}
