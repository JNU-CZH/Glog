package com.douyuehan.doubao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.douyuehan.doubao.common.exception.ApiAsserts;
import com.douyuehan.doubao.jwt.JwtUtil;
import com.douyuehan.doubao.mapper.BmsFollowMapper;
import com.douyuehan.doubao.mapper.BmsTopicMapper;
import com.douyuehan.doubao.mapper.UmsUserMapper;
import com.douyuehan.doubao.model.dto.LoginDTO;
import com.douyuehan.doubao.model.dto.RegisterDTO;
import com.douyuehan.doubao.model.entity.BmsFollow;
import com.douyuehan.doubao.model.entity.BmsPost;
import com.douyuehan.doubao.model.entity.UmsUser;
import com.douyuehan.doubao.model.vo.ProfileVO;
import com.douyuehan.doubao.service.IUmsUserService;
import com.douyuehan.doubao.utils.HostHolder;
import com.douyuehan.doubao.utils.MD5Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class IUmsUserServiceImpl extends ServiceImpl<UmsUserMapper, UmsUser> implements IUmsUserService {

    @Autowired
    private BmsTopicMapper bmsTopicMapper;
    @Autowired
    private BmsFollowMapper bmsFollowMapper;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public UmsUser executeRegister(RegisterDTO dto) {
        //查询是否有相同用户名的用户
        LambdaQueryWrapper<UmsUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsUser::getUsername, dto.getName()).or().eq(UmsUser::getEmail, dto.getEmail());
        UmsUser umsUser = baseMapper.selectOne(wrapper);
        if (!ObjectUtils.isEmpty(umsUser)) {
            ApiAsserts.fail("账号或邮箱已存在！");
        }
        UmsUser addUser = UmsUser.builder()
                .username(dto.getName())
                .alias(dto.getName())
                .password(MD5Utils.getPwd(dto.getPass()))
                .email(dto.getEmail())
                .createTime(new Date())
                .status(true)
                .build();
        baseMapper.insert(addUser);

        return addUser;
    }
    @Override
    public UmsUser getUserByUsername(String username) {
        return baseMapper.selectOne(new LambdaQueryWrapper<UmsUser>().eq(UmsUser::getUsername, username));
    }
    @Override
    public HashMap<String, String> executeLogin(LoginDTO dto) {
        HashMap<String, String> map = new HashMap<String, String>();
        String accessToken = null;
        String refreshToken = null;
        try {
            // 检查用户是否存在
            UmsUser user = getUserByUsername(dto.getUsername());
            String encodePwd = MD5Utils.getPwd(dto.getPassword());
            // 判断密码是否正确
            if(!encodePwd.equals(user.getPassword()))
            {
                throw new Exception("密码错误");
            }
            // 生成access_token
            accessToken = JwtUtil.generateAccessToken(String.valueOf(user.getUsername()));
            // 生成refresh_token
            refreshToken = JwtUtil.generateRefreshToken(String.valueOf(user.getUsername()));
            map.put("access", accessToken);
            map.put("fresh", refreshToken);
        } catch (Exception e) {
            log.warn("用户不存在or密码验证失败=======>{}", dto.getUsername());
        }
        return map;
    }
    @Override
    public ProfileVO getUserProfile(String id) {
        ProfileVO profile = new ProfileVO();
        UmsUser user = baseMapper.selectById(id);
        BeanUtils.copyProperties(user, profile);
        // 用户文章数
        int count = bmsTopicMapper.selectCount(new LambdaQueryWrapper<BmsPost>().eq(BmsPost::getUserId, id));
        profile.setTopicCount(count);

        // 粉丝数
        int followers = bmsFollowMapper.selectCount((new LambdaQueryWrapper<BmsFollow>().eq(BmsFollow::getParentId, id)));
        profile.setFollowerCount(followers);

        return profile;
    }
}
