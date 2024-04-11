package com.douyuehan.doubao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.douyuehan.doubao.mapper.BmsTagMapper;
import com.douyuehan.doubao.mapper.BmsTopicMapper;
import com.douyuehan.doubao.mapper.UmsUserMapper;
import com.douyuehan.doubao.model.dto.CreateTopicDTO;
import com.douyuehan.doubao.model.entity.BmsPost;
import com.douyuehan.doubao.model.entity.BmsTag;
import com.douyuehan.doubao.model.entity.BmsTopicTag;
import com.douyuehan.doubao.model.entity.UmsUser;
import com.douyuehan.doubao.model.vo.PostVO;
import com.douyuehan.doubao.model.vo.ProfileVO;
import com.douyuehan.doubao.service.IBmsPostService;
import com.douyuehan.doubao.service.IBmsTagService;
import com.douyuehan.doubao.service.IUmsUserService;
import com.douyuehan.doubao.utils.HostHolder;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class IBmsPostServiceImpl extends ServiceImpl<BmsTopicMapper, BmsPost> implements IBmsPostService {
    @Resource
    private BmsTagMapper bmsTagMapper;
    @Resource
    private UmsUserMapper umsUserMapper;

    @Autowired
    @Lazy
    private IBmsTagService iBmsTagService;

    @Autowired
    private IUmsUserService iUmsUserService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private HostHolder hostHolder;

    @Autowired
    private com.douyuehan.doubao.service.IBmsTopicTagService IBmsTopicTagService;
    @Override
    public Page<PostVO> getList(Page<PostVO> page, String tab) {
        System.out.println("现在时间" + new Date());
        Page<PostVO> iPage = new Page<>();
        if (!tab.equals("hot")) { // 如果是最新数据，走数据库查询
            // 查询话题
            iPage = this.baseMapper.selectListAndPage(page, tab);
        } else {
            // 查询热门数据
            List<PostVO> hotPosts = (List) redisTemplate.opsForList().range("hot_list", 0, -1);

            if (hotPosts == null) { //查数据库
                iPage = this.baseMapper.selectHotList(page, tab);
            } else {
                System.out.println("我使用的是redis哦");
                iPage.setRecords(hotPosts);
                iPage.setTotal(hotPosts.size());
                iPage.setCurrent(1);
            }
        }
        // 浏览量的处理
        // todo：是否要这边处理，或是直接redis异步刷盘就好
        for (PostVO post : iPage.getRecords()) {
            // 根据文章id从Redis获取浏览量
            String viewCountKey = "view" + post.getId();
            Integer viewCount = (Integer) redisTemplate.opsForValue().get(viewCountKey);

            // 如果Redis中没有浏览量记录，则初始化为0
            if (viewCount == null) {
                viewCount = 0;
            }

            // 更新文章的浏览量
            post.setView(viewCount);
            // 在Redis中重置浏览量
            redisTemplate.opsForValue().set(viewCountKey, viewCount);
        }
        // 查询话题的标签
        setTopicTags(iPage);
        System.out.println("方法结束时间" + new Date());
        return iPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BmsPost create(CreateTopicDTO dto, UmsUser user) {
        BmsPost topic1 = this.baseMapper.selectOne(new LambdaQueryWrapper<BmsPost>().eq(BmsPost::getTitle, dto.getTitle()));
        Assert.isNull(topic1, "话题已存在，请修改");


        // 封装
        BmsPost topic = BmsPost.builder()
                .userId(user.getId())
                .title(dto.getTitle())
                .content(EmojiParser.parseToAliases(dto.getContent()))
                .createTime(new Date())
                .build();
        this.baseMapper.insert(topic);

        // 使用ZSet存储文章id : 文章分数
        // todo: 可能存在问题，topic的id还未拿到
        // todo：如果刷新文章的时候不会每次都刷新到无用的文章
        redisTemplate.opsForSet().add("active_topic", topic.getId());

        // 用户积分增加
        int newScore = user.getScore() + 1;
        umsUserMapper.updateById(user.setScore(newScore));

        // 标签
        if (!ObjectUtils.isEmpty(dto.getTags())) {
            // 保存标签
            List<BmsTag> tags = iBmsTagService.insertTags(dto.getTags());
            // 处理标签与话题的关联
            IBmsTopicTagService.createTopicTag(topic.getId(), tags);
        }

        return topic;
    }

    /**
     * 获取文章详情
     * */
    @Override
    public Map<String, Object> viewTopic(String id, String username) {
        Map<String, Object> map = new HashMap<>(16);
        // 查询话题详情
        BmsPost topic = this.baseMapper.selectById(id);
        // 添加到文章活跃列表 --- 要更新分数
        redisTemplate.opsForSet().add("active_topic", id);
        Assert.notNull(topic, "当前话题不存在,或已被作者删除");
//        topic.setView(topic.getView() + 1);
        // 浏览量和点赞量，按照2个小时刷盘一次
        redisTemplate.opsForValue().increment("view" + id);
        map.put("view", redisTemplate.opsForValue().get("view" + id));
        // todo: 点赞状态（抽离出来充当点赞状态判断）
        // 点赞状态：通过set数据结构来存储，key：文章id，value：set（userId、userId2.....）
        Boolean member = redisTemplate.opsForSet().isMember(id, username);
        // 存在的话
        if (member) {
            map.put("likeStatus", 1);
        } else {
            map.put("likeStatus", 0);
        }
        long count = redisTemplate.opsForSet().size(id);
        topic.setLikeCount((int) count);
        map.put("likeCount", redisTemplate.opsForSet().size(id));
        this.baseMapper.updateById(topic);
        // emoji转码
        topic.setContent(EmojiParser.parseToUnicode(topic.getContent()));
        map.put("topic", topic);
        // 标签
        QueryWrapper<BmsTopicTag> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(BmsTopicTag::getTopicId, topic.getId());
        Set<String> set = new HashSet<>();
        for (BmsTopicTag articleTag : IBmsTopicTagService.list(wrapper)) {
            set.add(articleTag.getTagId());
        }
        List<BmsTag> tags = iBmsTagService.listByIds(set);
        map.put("tags", tags);

        // 作者

        ProfileVO user = iUmsUserService.getUserProfile(topic.getUserId());
        map.put("user", user);

        return map;
    }

    @Override
    public Map<String, Object> like(String id, String username, Integer newStatus) {
        Map<String, Object> map = new HashMap<>(2);
        // 判断当前状态
        if (newStatus == 0) {
            redisTemplate.opsForSet().add(id, username);
            map.put("likeStatus", 1);

        } else {
            redisTemplate.opsForSet().remove(id, username);
            map.put("likeStatus", 0);
        }
        map.put("likeCount", redisTemplate.opsForSet().size(id));
        return map;
    }

    @Override
    public List<BmsPost> getRecommend(String id) {
        return this.baseMapper.selectRecommend(id);
    }
    @Override
    public Page<PostVO> searchByKey(String keyword, Page<PostVO> page) {
        // 查询话题
        Page<PostVO> iPage = this.baseMapper.searchByKey(page, keyword);
        // 查询话题的标签
        setTopicTags(iPage);
        return iPage;
    }

    @Override
    public BmsPost selectById(String id) {
        return this.baseMapper.selectById(id);
    }

    @Override
    public List<BmsPost> selectBatchIds(Set ids) {
        QueryWrapper<BmsPost> queryWrapper = new QueryWrapper<>();
        ArrayList<String> idList= new ArrayList<>(ids);
        queryWrapper.in("id", idList);
        return this.baseMapper.selectList(queryWrapper);
    }

    private void setTopicTags(Page<PostVO> iPage) {
        iPage.getRecords().forEach(topic -> {
            List<BmsTopicTag> topicTags = IBmsTopicTagService.selectByTopicId(topic.getId());
            if (!topicTags.isEmpty()) {
                List<String> tagIds = topicTags.stream().map(BmsTopicTag::getTagId).collect(Collectors.toList());
                List<BmsTag> tags = bmsTagMapper.selectBatchIds(tagIds);
                topic.setTags(tags);
            }
        });
    }
}
