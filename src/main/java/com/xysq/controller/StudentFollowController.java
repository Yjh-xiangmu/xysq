package com.xysq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xysq.common.Result;
import com.xysq.entity.*;
import com.xysq.mapper.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/student")
public class StudentFollowController {

    @Autowired private SysFollowMapper followMapper;
    @Autowired private SysStudentMapper studentMapper;
    @Autowired private SysPostLikeMapper postLikeMapper;
    @Autowired private SysCommentMapper commentMapper;
    @Autowired private SysPostMapper postMapper;

    // 关注 / 取消关注
    @PostMapping("/follow/toggle")
    public Result<Map<String, Object>> toggleFollow(Integer followedId, HttpSession session) {
        Object obj = session.getAttribute("user");
        if (!(obj instanceof SysStudent me)) return Result.error("请先登录");
        if (me.getId().equals(followedId)) return Result.error("不能关注自己");

        SysFollow exist = followMapper.selectOne(new QueryWrapper<SysFollow>()
                .eq("follower_id", me.getId()).eq("followed_id", followedId));
        boolean isNowFollowed;
        if (exist != null) {
            followMapper.deleteById(exist.getId());
            isNowFollowed = false;
        } else {
            SysFollow f = new SysFollow();
            f.setFollowerId(me.getId());
            f.setFollowedId(followedId);
            followMapper.insert(f);
            isNowFollowed = true;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("isFollowed", isNowFollowed);
        return Result.success(data);
    }

    // 我的关注列表
    @GetMapping("/follow/list")
    public Result<List<Map<String, Object>>> getFollowList(HttpSession session) {
        Object obj = session.getAttribute("user");
        if (!(obj instanceof SysStudent me)) return Result.error("请先登录");

        List<SysFollow> follows = followMapper.selectList(
                new QueryWrapper<SysFollow>().eq("follower_id", me.getId()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysFollow f : follows) {
            SysStudent s = studentMapper.selectById(f.getFollowedId());
            if (s == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("studentId", s.getId());
            m.put("nickname", s.getNickname());
            m.put("avatar", s.getAvatar());
            m.put("intro", s.getIntro());
            result.add(m);
        }
        return Result.success(result);
    }

    // 查看他人主页
    @GetMapping("/user/{id}/profile")
    public Result<Map<String, Object>> getUserProfile(@PathVariable Integer id, HttpSession session) {
        SysStudent s = studentMapper.selectById(id);
        if (s == null) return Result.error("用户不存在");

        long likeCount = postLikeMapper.selectCount(new QueryWrapper<SysPostLike>()
                .inSql("post_id", "SELECT id FROM sys_post WHERE student_id=" + id));
        long followCount = followMapper.selectCount(new QueryWrapper<SysFollow>().eq("follower_id", id));
        long followerCount = followMapper.selectCount(new QueryWrapper<SysFollow>().eq("followed_id", id));

        // 最近评论（最多10条）
        List<SysComment> comments = commentMapper.selectList(new QueryWrapper<SysComment>()
                .eq("student_id", id).orderByDesc("create_time").last("LIMIT 10"));
        List<Map<String, Object>> commentList = new ArrayList<>();
        for (SysComment c : comments) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("content", c.getContent());
            cm.put("createTime", c.getCreateTime());
            commentList.add(cm);
        }

        boolean isFollowed = false;
        Object obj = session.getAttribute("user");
        if (obj instanceof SysStudent me) {
            isFollowed = followMapper.selectCount(new QueryWrapper<SysFollow>()
                    .eq("follower_id", me.getId()).eq("followed_id", id)) > 0;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("studentId", s.getId());
        data.put("nickname", s.getNickname());
        data.put("avatar", s.getAvatar());
        data.put("intro", s.getIntro());
        data.put("phone", s.getPhone());
        data.put("email", s.getEmail());
        data.put("likeCount", likeCount);
        data.put("followCount", followCount);
        data.put("followerCount", followerCount);
        data.put("comments", commentList);
        data.put("isFollowed", isFollowed);
        return Result.success(data);
    }

    // 我的粉丝列表（谁关注了我）
    @GetMapping("/follow/followers")
    public Result<List<Map<String, Object>>> getFollowerList(HttpSession session) {
        Object obj = session.getAttribute("user");
        if (!(obj instanceof SysStudent me)) return Result.error("请先登录");

        List<SysFollow> follows = followMapper.selectList(
                new QueryWrapper<SysFollow>().eq("followed_id", me.getId()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysFollow f : follows) {
            SysStudent s = studentMapper.selectById(f.getFollowerId());
            if (s == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("studentId", s.getId());
            m.put("nickname", s.getNickname());
            m.put("avatar", s.getAvatar());
            m.put("intro", s.getIntro());
            result.add(m);
        }
        return Result.success(result);
    }

    // 检查是否关注某人（用于页面初始化按钮状态）
    @GetMapping("/follow/check")
    public Result<Map<String, Object>> checkFollow(Integer followedId, HttpSession session) {
        Object obj = session.getAttribute("user");
        if (!(obj instanceof SysStudent me)) {
            Map<String, Object> data = new HashMap<>();
            data.put("isFollowed", false);
            return Result.success(data);
        }
        boolean isFollowed = followMapper.selectCount(new QueryWrapper<SysFollow>()
                .eq("follower_id", me.getId()).eq("followed_id", followedId)) > 0;
        Map<String, Object> data = new HashMap<>();
        data.put("isFollowed", isFollowed);
        return Result.success(data);
    }
}
