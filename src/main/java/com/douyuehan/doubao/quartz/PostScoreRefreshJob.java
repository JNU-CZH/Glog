package com.douyuehan.doubao.quartz;

import com.douyuehan.doubao.model.entity.BmsPost;
import com.douyuehan.doubao.model.vo.PostVO;
import com.douyuehan.doubao.model.vo.ProfileVO;
import com.douyuehan.doubao.service.IBmsPostService;
import com.douyuehan.doubao.service.IUmsUserService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 定时任务：刷新文章分数
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/3/23 19:59
 **/
public class PostScoreRefreshJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private IBmsPostService ibMsPostService;

    @Resource
    private IUmsUserService iUmsUserService;

    // 牛客纪元
    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败!", e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        /* --------------------------------------刷新活跃文章--------------------------------------------*/
        // 获取redis操作对象：文章id对应分数的ZSet集合
        BoundSetOperations operations = redisTemplate.boundSetOps("active_topic");

        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数: " + operations.size());
        // 获取ZSet中的所有元素
        Set<String> postIdSet = operations.members();

        // 遍历所有元素，刷新其分数（数据库）
        while (operations.size() > 0) {
            this.refresh((String) operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
        /* --------------------------------------实现热门文章缓存--------------------------------------------*/

        // 1、获取排名前10的文章
        BoundZSetOperations totalTopic = redisTemplate.boundZSetOps("total_topic");
        // 热门文章id集合
        Set set;
        if (totalTopic.size() < 10) {
            set = totalTopic.range(0, totalTopic.size() - 1);
        } else {
            set = totalTopic.range(0, 9);
        }
        // 删除旧的热门文章
        redisTemplate.delete("hot_list");
        // 查找ids集合对应的文章信息
        List<PostVO> postVOS = new ArrayList<>();
        List<BmsPost> hotPosts = ibMsPostService.selectBatchIds(set);
        for (BmsPost bmsPost : hotPosts) {
            ProfileVO userProfile = iUmsUserService.getUserProfile(bmsPost.getUserId());
            PostVO postVO = getPostVO(bmsPost, userProfile);
            redisTemplate.opsForList().leftPush("hot_list", postVO);
            postVOS.add(postVO);
        }

    }

    private static PostVO getPostVO(BmsPost bmsPost, ProfileVO userProfile) {
        PostVO postVO = new PostVO();
        postVO.setId(bmsPost.getId());
        postVO.setUserId(bmsPost.getUserId());
        postVO.setAvatar(userProfile.getAvatar());
        postVO.setAlias(userProfile.getAlias());
        postVO.setUsername(userProfile.getUsername());
        postVO.setTitle(bmsPost.getTitle());
        postVO.setComments(bmsPost.getComments());
        postVO.setView(bmsPost.getView());
        postVO.setLikeCount(bmsPost.getLikeCount());
        postVO.setCreateTime(bmsPost.getCreateTime());
        return postVO;
    }

    private void refresh(String postId) {
        // 从数据库中查询帖子
        BmsPost post = ibMsPostService.selectById(postId);
        if (post == null) {
            logger.error("该帖子不存在: id = " + postId);
            return;
        }
        // 是否精华
//         boolean wonderful = post.getStatus() == 1;
        // 评论数量
        int commentCount = post.getComments();
        // 点赞数量
        long likeCount = redisTemplate.opsForSet().size(postId);
        // 计算权重
        double w = commentCount * 10 + likeCount * 2;
        // 分数 = 帖子权重 + 发帖时间
        double score = Math.log10(Math.max(w, 1))
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        post.setScore(score);
        // 更新帖子分数
        ibMsPostService.updateById(post);

        // 文章分数排名
        BoundZSetOperations totalTopic = redisTemplate.boundZSetOps("total_topic");
        totalTopic.add(postId, score);

        // todo : es同步搜索数据
//         post.setScore(score);
//         elasticsearchService.saveDiscussPost(post);
    }
}