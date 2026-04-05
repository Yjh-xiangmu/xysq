package com.xysq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xysq.common.Result;
import com.xysq.entity.*;
import com.xysq.mapper.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/community")
public class CommunityAdminController {

    @Autowired private SysCommunityMemberMapper memberMapper;
    @Autowired private SysStudentMapper studentMapper;
    @Autowired private SysActivityMapper activityMapper;
    @Autowired private SysActivitySignMapper activitySignMapper;
    @Autowired private SysPostMapper postMapper;
    @Autowired private SysCommentMapper commentMapper;
    @Autowired private SysCommunityMapper communityMapper;
    @Autowired private SysAdminMapper adminMapper;

    // ===== 权限校验工具 =====
    private SysAdmin getAdmin(HttpSession session) {
        Object obj = session.getAttribute("user");
        if (obj instanceof SysAdmin admin && admin.getRole() == 2) return admin;
        return null;
    }

    // ===================== 成员管理 =====================

    // 待审核列表
    @GetMapping("/member/auditList")
    public Result<List<Map<String, Object>>> getAuditList(HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        if (admin.getCommunityId() == null) return Result.error("您的账号尚未绑定任何社群");

        List<SysCommunityMember> records = memberMapper.selectList(
                new QueryWrapper<SysCommunityMember>().eq("community_id", admin.getCommunityId()).eq("status", 0));

        List<Map<String, Object>> list = new ArrayList<>();
        for (SysCommunityMember r : records) {
            SysStudent s = studentMapper.selectById(r.getStudentId());
            if (s != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("recordId", r.getId());
                m.put("studentNo", s.getStudentNo());
                m.put("nickname", s.getNickname());
                m.put("joinTime", r.getJoinTime());
                list.add(m);
            }
        }
        return Result.success(list);
    }

    // 审核（同意/拒绝）
    @PostMapping("/member/audit")
    public Result<?> auditMember(Integer recordId, Integer status, HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        SysCommunityMember record = memberMapper.selectById(recordId);
        if (record == null) return Result.error("申请记录不存在");
        if (!record.getCommunityId().equals(admin.getCommunityId())) return Result.error("越权操作");
        record.setStatus(status);
        memberMapper.updateById(record);
        return Result.success("审核操作成功");
    }

    // 已加入成员列表
    @GetMapping("/member/joinedList")
    public Result<List<Map<String, Object>>> getJoinedList(HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        if (admin.getCommunityId() == null) return Result.error("未绑定社群");

        List<SysCommunityMember> records = memberMapper.selectList(
                new QueryWrapper<SysCommunityMember>().eq("community_id", admin.getCommunityId()).eq("status", 1));

        List<Map<String, Object>> list = new ArrayList<>();
        for (SysCommunityMember r : records) {
            SysStudent s = studentMapper.selectById(r.getStudentId());
            if (s != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("recordId", r.getId());
                m.put("studentId", s.getId());
                m.put("studentNo", s.getStudentNo());
                m.put("nickname", s.getNickname());
                m.put("joinTime", r.getJoinTime());
                list.add(m);
            }
        }
        return Result.success(list);
    }

    // 踢出成员
    @PostMapping("/member/remove")
    public Result<?> removeMember(Integer recordId, HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        SysCommunityMember record = memberMapper.selectById(recordId);
        if (record == null) return Result.error("成员记录不存在");
        if (!record.getCommunityId().equals(admin.getCommunityId())) return Result.error("越权操作");
        memberMapper.deleteById(recordId);
        return Result.success("已将该成员移出社群");
    }

    // ===================== 帖子管理 =====================

    // 获取本社群所有帖子
    @GetMapping("/post/list")
    public Result<List<Map<String, Object>>> getPostList(HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        if (admin.getCommunityId() == null) return Result.error("未绑定社群");

        List<SysPost> posts = postMapper.selectList(
                new QueryWrapper<SysPost>().eq("community_id", admin.getCommunityId()).orderByDesc("create_time"));

        List<Map<String, Object>> list = new ArrayList<>();
        for (SysPost p : posts) {
            SysStudent s = studentMapper.selectById(p.getStudentId());
            long commentCount = commentMapper.selectCount(new QueryWrapper<SysComment>().eq("post_id", p.getId()));
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("content", p.getContent());
            m.put("status", p.getStatus());
            m.put("createTime", p.getCreateTime());
            m.put("authorName", s != null ? s.getNickname() : "匿名");
            m.put("commentCount", commentCount);
            list.add(m);
        }
        return Result.success(list);
    }

    // 切换帖子状态（隐藏/显示）
    @PostMapping("/post/toggleStatus")
    public Result<?> togglePostStatus(Integer postId, HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        SysPost post = postMapper.selectById(postId);
        if (post == null) return Result.error("帖子不存在");
        if (!post.getCommunityId().equals(admin.getCommunityId())) return Result.error("越权操作");
        post.setStatus(post.getStatus() == 1 ? 0 : 1);
        postMapper.updateById(post);
        return Result.success(post.getStatus() == 1 ? "帖子已恢复显示" : "帖子已隐藏");
    }

    // 删除帖子（同时删评论）
    @PostMapping("/post/delete")
    public Result<?> deletePost(Integer postId, HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        SysPost post = postMapper.selectById(postId);
        if (post == null) return Result.error("帖子不存在");
        if (!post.getCommunityId().equals(admin.getCommunityId())) return Result.error("越权操作");
        commentMapper.delete(new QueryWrapper<SysComment>().eq("post_id", postId));
        postMapper.deleteById(postId);
        return Result.success("删除成功");
    }

    // ===================== 活动管理 =====================

    @PostMapping("/activity/publish")
    public Result<?> publishActivity(String title, String content, String location,
                                     @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date eventTime,
                                     HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        if (admin.getCommunityId() == null) return Result.error("未绑定社群，无法发布");
        SysActivity activity = new SysActivity();
        activity.setCommunityId(admin.getCommunityId());
        activity.setTitle(title);
        activity.setContent(content);
        activity.setLocation(location);
        activity.setEventTime(eventTime);
        activityMapper.insert(activity);
        return Result.success("活动发布成功！");
    }

    // ===================== 社群信息 =====================

    @GetMapping("/info")
    public Result<SysCommunity> getCommunityInfo(HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        if (admin.getCommunityId() == null) return Result.error("未绑定社群");
        SysCommunity c = communityMapper.selectById(admin.getCommunityId());
        return Result.success(c);
    }

    @PostMapping("/info/update")
    public Result<?> updateCommunityInfo(String name, String description, HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        if (admin.getCommunityId() == null) return Result.error("未绑定社群");
        SysCommunity c = communityMapper.selectById(admin.getCommunityId());
        if (c == null) return Result.error("社群不存在");
        if (name != null && !name.trim().isEmpty()) c.setName(name.trim());
        if (description != null) c.setDescription(description.trim());
        communityMapper.updateById(c);
        return Result.success("社群信息更新成功");
    }

    // ===================== 活动管理 扩展 =====================

    // 获取本社群活动列表
    @GetMapping("/activity/list")
    public Result<List<Map<String, Object>>> getActivityList(HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        if (admin.getCommunityId() == null) return Result.error("未绑定社群");

        List<SysActivity> activities = activityMapper.selectList(
                new QueryWrapper<SysActivity>().eq("community_id", admin.getCommunityId()).orderByDesc("create_time"));

        List<Map<String, Object>> list = new ArrayList<>();
        for (SysActivity a : activities) {
            long signCount = activitySignMapper.selectCount(
                    new QueryWrapper<SysActivitySign>().eq("activity_id", a.getId()));
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("location", a.getLocation());
            m.put("eventTime", a.getEventTime());
            m.put("createTime", a.getCreateTime());
            m.put("signCount", signCount);
            list.add(m);
        }
        return Result.success(list);
    }

    // 获取某场活动的报名名单
    @GetMapping("/activity/signList")
    public Result<List<Map<String, Object>>> getSignList(Integer activityId, HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        SysActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getCommunityId().equals(admin.getCommunityId()))
            return Result.error("无权限查看该活动");

        List<SysActivitySign> signs = activitySignMapper.selectList(
                new QueryWrapper<SysActivitySign>().eq("activity_id", activityId).orderByAsc("sign_time"));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysActivitySign s : signs) {
            SysStudent st = studentMapper.selectById(s.getStudentId());
            Map<String, Object> m = new HashMap<>();
            m.put("studentNo", st != null ? st.getStudentNo() : "-");
            m.put("nickname", st != null ? st.getNickname() : "-");
            m.put("signTime", s.getSignTime());
            list.add(m);
        }
        return Result.success(list);
    }

    // 删除活动
    @PostMapping("/activity/delete")
    public Result<?> deleteActivity(Integer activityId, HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        SysActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getCommunityId().equals(admin.getCommunityId()))
            return Result.error("越权操作");
        activitySignMapper.delete(new QueryWrapper<SysActivitySign>().eq("activity_id", activityId));
        activityMapper.deleteById(activityId);
        return Result.success("活动已删除");
    }

    // ===================== 评论管理 =====================

    // 获取本社群所有帖子的评论（评论筛选）
    @GetMapping("/comment/list")
    public Result<List<Map<String, Object>>> getCommentList(HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        if (admin.getCommunityId() == null) return Result.error("未绑定社群");

        // 取本社群帖子
        List<SysPost> posts = postMapper.selectList(
                new QueryWrapper<SysPost>().eq("community_id", admin.getCommunityId()));
        if (posts.isEmpty()) return Result.success(Collections.emptyList());

        List<Integer> postIds = posts.stream().map(SysPost::getId).collect(java.util.stream.Collectors.toList());
        Map<Integer, String> postPreviewMap = new HashMap<>();
        posts.forEach(p -> postPreviewMap.put(p.getId(),
                p.getContent().length() > 20 ? p.getContent().substring(0, 20) + "..." : p.getContent()));

        List<SysComment> comments = commentMapper.selectList(
                new QueryWrapper<SysComment>().in("post_id", postIds).orderByDesc("create_time"));

        // 批量查学生信息
        Set<Integer> sids = new HashSet<>();
        comments.forEach(c -> sids.add(c.getStudentId()));
        Map<Integer, String> nameMap = new HashMap<>();
        if (!sids.isEmpty()) studentMapper.selectBatchIds(sids).forEach(s -> nameMap.put(s.getId(), s.getNickname()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (SysComment c : comments) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("content", c.getContent());
            m.put("createTime", c.getCreateTime());
            m.put("authorName", nameMap.getOrDefault(c.getStudentId(), "匿名"));
            m.put("postPreview", postPreviewMap.getOrDefault(c.getPostId(), ""));
            m.put("isReply", c.getParentId() != null && c.getParentId() != 0);
            list.add(m);
        }
        return Result.success(list);
    }

    // 删除评论
    @PostMapping("/comment/delete")
    public Result<?> deleteComment(Integer commentId, HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        SysComment comment = commentMapper.selectById(commentId);
        if (comment == null) return Result.error("评论不存在");
        // 验证该评论属于本社群帖子
        SysPost post = postMapper.selectById(comment.getPostId());
        if (post == null || !post.getCommunityId().equals(admin.getCommunityId()))
            return Result.error("越权操作");
        // 同时删除该评论下的回复
        commentMapper.delete(new QueryWrapper<SysComment>().eq("parent_id", commentId));
        commentMapper.deleteById(commentId);
        return Result.success("评论已删除");
    }

    // ===================== 管理员密码修改 =====================

    @PostMapping("/admin/pwd/update")
    public Result<?> updatePassword(String oldPwd, String newPwd, HttpSession session) {
        SysAdmin admin = getAdmin(session);
        if (admin == null) return Result.error("无权限或未登录");
        SysAdmin dbAdmin = adminMapper.selectById(admin.getId());
        if (!dbAdmin.getPassword().equals(oldPwd)) return Result.error("原密码错误");
        if (newPwd == null || newPwd.length() < 6) return Result.error("新密码不能少于6位");
        dbAdmin.setPassword(newPwd);
        adminMapper.updateById(dbAdmin);
        return Result.success("密码修改成功，请重新登录");
    }
}
