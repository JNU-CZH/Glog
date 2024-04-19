package com.douyuehan.doubao.controller;

import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.douyuehan.doubao.common.api.ApiResult;
import com.douyuehan.doubao.model.dto.CommentDTO;
import com.douyuehan.doubao.model.dto.CreateTopicDTO;
import com.douyuehan.doubao.model.entity.BmsComment;
import com.douyuehan.doubao.model.entity.BmsPost;
import com.douyuehan.doubao.model.entity.UmsUser;
import com.douyuehan.doubao.model.vo.PostVO;
import com.douyuehan.doubao.service.IBmsCommentService;
import com.douyuehan.doubao.service.IBmsPostService;
import com.douyuehan.doubao.service.IUmsUserService;
import com.douyuehan.doubao.utils.BloomFilterUtil;
import com.vdurmont.emoji.EmojiParser;
import org.redisson.Redisson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.douyuehan.doubao.jwt.JwtUtil.USER_NAME;


@RestController
@RequestMapping("/post")
public class BmsPostController extends BaseController {

    @Resource
    private IBmsPostService iBmsPostService;
    @Resource
    private IUmsUserService umsUserService;

    @Resource
    private BloomFilterUtil bloomFilter;

    @GetMapping("/list")
    public ApiResult<Page<PostVO>> list(@RequestParam(value = "tab", defaultValue = "latest") String tab,
                                        @RequestParam(value = "pageNo", defaultValue = "1")  Integer pageNo,
                                        @RequestParam(value = "size", defaultValue = "10") Integer pageSize) throws InterruptedException {
        Page<PostVO> list = iBmsPostService.getList(new Page<>(pageNo, pageSize), tab);
        return ApiResult.success(list);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @Transactional(rollbackFor = RuntimeException.class)
    public ApiResult<BmsPost> create(@RequestHeader(value = USER_NAME) String userName
            , @RequestBody CreateTopicDTO dto) {
        UmsUser user = umsUserService.getUserByUsername(userName);
        BmsPost topic = iBmsPostService.create(dto, user);
        if (!bloomFilter.isIn(topic.getId())) {
            bloomFilter.addItem(topic.getId());
        }
        return ApiResult.success(topic);
    }



    /**
     * 获取文章详情
     *
     * @param id 文章id
     * @return ApiResult<Map<String, Object>> map存储基本信息
     * */
    @GetMapping()
    public ApiResult<Map<String, Object>> view(@RequestParam("id") String id, @RequestParam("username") String username) {
        boolean isIn = bloomFilter.isIn(id);
        if (isIn == false) {
            return ApiResult.failed("文章不存在，请访问正确文章");
        }
        Map<String, Object> map = iBmsPostService.viewTopic(id, username);
        return ApiResult.success(map);
    }

    /**
     * 点赞文章与取消点赞
     *
     * @param id,username,newStatus 文章id、用户id、新状态
     * @return ApiResult<Map<String, Object>> map存储基本信息
     * */
    @GetMapping("/like")
    public ApiResult<Map<String, Object>> like(@RequestParam("id") String id, @RequestParam("username") String username, @RequestParam("newStatus") Integer newStatus) {
        Map<String, Object> map = iBmsPostService.like(id, username, newStatus);
        return ApiResult.success(map);
    }

    @GetMapping("/recommend")
    public ApiResult<List<BmsPost>> getRecommend(@RequestParam("topicId") String id) {
        List<BmsPost> topics = iBmsPostService.getRecommend(id);
        return ApiResult.success(topics);
    }

    @PostMapping("/update")
    public ApiResult<BmsPost> update(@RequestHeader(value = USER_NAME) String userName, @Valid @RequestBody BmsPost post) {
        UmsUser umsUser = umsUserService.getUserByUsername(userName);
        Assert.isTrue(umsUser.getId().equals(post.getUserId()), "非本人无权修改");
        post.setModifyTime(new Date());
        post.setContent(EmojiParser.parseToAliases(post.getContent()));
        iBmsPostService.updateById(post);
        return ApiResult.success(post);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResult<String> delete(@RequestHeader(value = USER_NAME) String userName, @PathVariable("id") String id) {
        UmsUser umsUser = umsUserService.getUserByUsername(userName);
        BmsPost byId = iBmsPostService.getById(id);
        Assert.notNull(byId, "来晚一步，话题已不存在");
        Assert.isTrue(byId.getUserId().equals(umsUser.getId()), "你为什么可以删除别人的话题？？？");
        iBmsPostService.removeById(id);
        return ApiResult.success(null,"删除成功");
    }

}
