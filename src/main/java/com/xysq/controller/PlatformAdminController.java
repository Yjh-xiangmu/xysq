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
@RequestMapping("/api/platform")
public class PlatformAdminController {

    @Autowired private SysStudentMapper studentMapper;
    @Autowired private SysCommunityMapper communityMapper;
    @Autowired private SysCommunityMemberMapper memberMapper;
    @Autowired private SysPostMapper postMapper;
    @Autowired private SysCommentMapper commentMapper;
    @Autowired private SysActivityMapper activityMapper;
    @Autowired private SysAnnouncementMapper announcementMapper;
    @Autowired private SysAdminMapper adminMapper;

    private boolean isPlatformAdmin(HttpSession session) {
        Object obj = session.getAttribute("user");
        return obj instanceof SysAdmin admin && admin.getRole() == 1;
    }

    // 总体统计数据
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        Map<String, Object> stats = new HashMap<>();
        stats.put("studentCount", studentMapper.selectCount(null));
        stats.put("communityCount", communityMapper.selectCount(null));
        stats.put("postCount", postMapper.selectCount(null));
        stats.put("activityCount", activityMapper.selectCount(null));
        stats.put("commentCount", commentMapper.selectCount(null));
        return Result.success(stats);
    }

    // 各社群帖子/成员数量（图表用）
    @GetMapping("/community/stats")
    public Result<List<Map<String, Object>>> getCommunityStats(HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        List<SysCommunity> communities = communityMapper.selectList(null);
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysCommunity c : communities) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", c.getName());
            m.put("postCount", postMapper.selectCount(new QueryWrapper<SysPost>().eq("community_id", c.getId())));
            m.put("memberCount", memberMapper.selectCount(
                    new QueryWrapper<SysCommunityMember>().eq("community_id", c.getId()).eq("status", 1)));
            list.add(m);
        }
        return Result.success(list);
    }

    // 学生列表
    @GetMapping("/student/list")
    public Result<List<Map<String, Object>>> getStudentList(HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        List<SysStudent> students = studentMapper.selectList(new QueryWrapper<SysStudent>().orderByDesc("id"));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysStudent s : students) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("studentNo", s.getStudentNo());
            m.put("nickname", s.getNickname());
            m.put("joinedCommunities", memberMapper.selectCount(
                    new QueryWrapper<SysCommunityMember>().eq("student_id", s.getId()).eq("status", 1)));
            m.put("postCount", postMapper.selectCount(new QueryWrapper<SysPost>().eq("student_id", s.getId())));
            m.put("status", s.getStatus() == null ? 1 : s.getStatus());
            list.add(m);
        }
        return Result.success(list);
    }

    @PostMapping("/student/delete")
    public Result<?> deleteStudent(Integer studentId, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        studentMapper.deleteById(studentId);
        return Result.success("已删除该学生账号");
    }

    @PostMapping("/student/toggleStatus")
    public Result<Map<String, Object>> toggleStudentStatus(Integer studentId, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        SysStudent s = studentMapper.selectById(studentId);
        if (s == null) return Result.error("学生不存在");
        int newStatus = (s.getStatus() != null && s.getStatus() == 1) ? 0 : 1;
        s.setStatus(newStatus);
        studentMapper.updateById(s);
        Map<String, Object> data = new HashMap<>();
        data.put("status", newStatus);
        return Result.success(data);
    }

    // ===================== 社长管理 =====================
    @GetMapping("/admin/list")
    public Result<List<Map<String, Object>>> getAdminList(HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        List<SysAdmin> admins = adminMapper.selectList(new QueryWrapper<SysAdmin>().eq("role", 2).orderByAsc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysAdmin a : admins) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("username", a.getUsername());
            if (a.getCommunityId() != null) {
                SysCommunity c = communityMapper.selectById(a.getCommunityId());
                m.put("communityId", a.getCommunityId());
                m.put("communityName", c != null ? c.getName() : "已解散");
            } else {
                m.put("communityId", null);
                m.put("communityName", "未绑定社群");
            }
            result.add(m);
        }
        return Result.success(result);
    }

    @PostMapping("/admin/add")
    public Result<?> addAdmin(String username, String password, Integer communityId, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        if (username == null || username.trim().isEmpty()) return Result.error("用户名不能为空");
        if (password == null || password.trim().isEmpty()) return Result.error("密码不能为空");
        SysAdmin exist = adminMapper.selectOne(new QueryWrapper<SysAdmin>().eq("username", username.trim()));
        if (exist != null) return Result.error("该用户名已存在");
        SysAdmin admin = new SysAdmin();
        admin.setUsername(username.trim());
        admin.setPassword(password.trim());
        admin.setRole(2);
        admin.setCommunityId(communityId);
        adminMapper.insert(admin);
        return Result.success("社长账号创建成功");
    }

    @PostMapping("/admin/delete")
    public Result<?> deleteAdmin(Integer adminId, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        SysAdmin a = adminMapper.selectById(adminId);
        if (a == null) return Result.error("账号不存在");
        if (a.getRole() == 1) return Result.error("不能删除平台管理员");
        adminMapper.deleteById(adminId);
        return Result.success("社长账号已删除");
    }

    // ===================== 帖子审核 =====================
    @GetMapping("/post/list")
    public Result<List<Map<String, Object>>> getAllPosts(HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        List<SysPost> posts = postMapper.selectList(new QueryWrapper<SysPost>().orderByDesc("create_time"));
        Set<Integer> communityIds = new HashSet<>();
        Set<Integer> studentIds = new HashSet<>();
        posts.forEach(p -> { communityIds.add(p.getCommunityId()); studentIds.add(p.getStudentId()); });
        Map<Integer, String> communityNameMap = new HashMap<>();
        Map<Integer, String> studentNameMap = new HashMap<>();
        if (!communityIds.isEmpty()) communityMapper.selectBatchIds(communityIds).forEach(c -> communityNameMap.put(c.getId(), c.getName()));
        if (!studentIds.isEmpty()) studentMapper.selectBatchIds(studentIds).forEach(s -> studentNameMap.put(s.getId(), s.getNickname()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (SysPost p : posts) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("content", p.getContent());
            m.put("status", p.getStatus());
            m.put("createTime", p.getCreateTime());
            m.put("communityName", communityNameMap.getOrDefault(p.getCommunityId(), "未知社群"));
            m.put("authorName", studentNameMap.getOrDefault(p.getStudentId(), "匿名"));
            list.add(m);
        }
        return Result.success(list);
    }

    @PostMapping("/post/toggleStatus")
    public Result<?> togglePostStatus(Integer postId, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        SysPost post = postMapper.selectById(postId);
        if (post == null) return Result.error("帖子不存在");
        post.setStatus(post.getStatus() == 1 ? 0 : 1);
        postMapper.updateById(post);
        return Result.success(post.getStatus() == 1 ? "帖子已恢复显示" : "帖子已隐藏");
    }

    // ===================== 全站公告 =====================
    @GetMapping("/announcement/list")
    public Result<List<Map<String, Object>>> getAnnouncementList(HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        List<SysAnnouncement> list = announcementMapper.selectList(new QueryWrapper<SysAnnouncement>().orderByDesc("create_time"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysAnnouncement a : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId()); m.put("title", a.getTitle()); m.put("content", a.getContent()); m.put("createTime", a.getCreateTime());
            result.add(m);
        }
        return Result.success(result);
    }

    @PostMapping("/announcement/publish")
    public Result<?> publishAnnouncement(String title, String content, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        if (title == null || title.trim().isEmpty()) return Result.error("标题不能为空");
        if (content == null || content.trim().isEmpty()) return Result.error("内容不能为空");
        SysAdmin admin = (SysAdmin) session.getAttribute("user");
        SysAnnouncement a = new SysAnnouncement();
        a.setTitle(title.trim()); a.setContent(content.trim()); a.setAdminId(admin.getId());
        announcementMapper.insert(a);
        return Result.success("公告发布成功");
    }

    @PostMapping("/announcement/delete")
    public Result<?> deleteAnnouncement(Integer id, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        announcementMapper.deleteById(id);
        return Result.success("公告已删除");
    }

    // ===================== 社群管理（含新增/删除） =====================
    @GetMapping("/community/list")
    public Result<List<Map<String, Object>>> getCommunityList(HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        List<SysCommunity> list = communityMapper.selectList(new QueryWrapper<SysCommunity>().orderByAsc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysCommunity c : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("category", c.getCategory());
            m.put("isRecommended", c.getIsRecommended() != null && c.getIsRecommended() == 1);
            m.put("memberCount", memberMapper.selectCount(
                    new QueryWrapper<SysCommunityMember>().eq("community_id", c.getId()).eq("status", 1)));
            result.add(m);
        }
        return Result.success(result);
    }

    @PostMapping("/community/add")
    public Result<?> addCommunity(String name, String description, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        if (name == null || name.trim().isEmpty()) return Result.error("社群名称不能为空");
        long count = communityMapper.selectCount(new QueryWrapper<SysCommunity>().eq("name", name.trim()));
        if (count > 0) return Result.error("社群名称已存在");
        SysCommunity c = new SysCommunity();
        c.setName(name.trim());
        c.setDescription(description != null ? description.trim() : "");
        c.setAvatar("https://api.dicebear.com/7.x/bottts/svg?seed=" + System.currentTimeMillis());
        c.setIsRecommended(0);
        communityMapper.insert(c);
        return Result.success("社群创建成功");
    }

    @PostMapping("/community/delete")
    public Result<?> deleteCommunity(Integer communityId, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        communityMapper.deleteById(communityId);
        return Result.success("社群已删除");
    }

    @PostMapping("/community/setCategory")
    public Result<?> setCommunityCategory(Integer communityId, String category, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        SysCommunity c = communityMapper.selectById(communityId);
        if (c == null) return Result.error("社群不存在");
        c.setCategory(category != null && !category.trim().isEmpty() ? category.trim() : null);
        communityMapper.updateById(c);
        return Result.success("分类更新成功");
    }

    @PostMapping("/community/toggleRecommend")
    public Result<Map<String, Object>> toggleRecommend(Integer communityId, HttpSession session) {
        if (!isPlatformAdmin(session)) return Result.error("无权限");
        SysCommunity c = communityMapper.selectById(communityId);
        if (c == null) return Result.error("社群不存在");
        int newVal = (c.getIsRecommended() != null && c.getIsRecommended() == 1) ? 0 : 1;
        c.setIsRecommended(newVal);
        communityMapper.updateById(c);
        Map<String, Object> data = new HashMap<>();
        data.put("isRecommended", newVal == 1);
        return Result.success(data);
    }
}