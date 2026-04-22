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
public class StudentCommunityController {

    @Autowired private SysCommunityMapper communityMapper;
    @Autowired private SysCommunityMemberMapper memberMapper;
    @Autowired private SysAnnouncementMapper announcementMapper;
    @Autowired private SysStudentMapper studentMapper;
    @Autowired private SysAdminMapper adminMapper;
    @Autowired private SysReportMapper reportMapper;

    // 获取所有社群列表（广场）- 支持关键词和分类搜索
    @GetMapping("/community/list")
    public Result<List<Map<String, Object>>> getCommunityList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            HttpSession session) {

        SysStudent student = (SysStudent) session.getAttribute("user");
        Integer studentId = (student != null) ? student.getId() : null;

        QueryWrapper<SysCommunity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1); // 只显示已审核通过的社群
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("name", keyword.trim()).or().like("description", keyword.trim()));
        }
        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq("category", category.trim());
        }

        List<SysCommunity> list = communityMapper.selectList(wrapper);
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (SysCommunity c : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("description", c.getDescription());
            map.put("avatar", c.getAvatar());

            Integer joinStatus = -1;
            if (studentId != null) {
                SysCommunityMember member = memberMapper.selectOne(new QueryWrapper<SysCommunityMember>()
                        .eq("community_id", c.getId())
                        .eq("student_id", studentId));
                if (member != null) {
                    joinStatus = member.getStatus();
                }
            }
            map.put("joinStatus", joinStatus);
            map.put("isRecommended", c.getIsRecommended() != null && c.getIsRecommended() == 1);
            map.put("category", c.getCategory());
            resultList.add(map);
        }

        return Result.success(resultList);
    }

    // 获取最新公告（最多5条，展示给所有用户）
    @GetMapping("/announcements")
    public Result<List<Map<String, Object>>> getAnnouncements() {
        List<SysAnnouncement> list = announcementMapper.selectList(
                new QueryWrapper<SysAnnouncement>().orderByDesc("create_time").last("LIMIT 5"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysAnnouncement a : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("content", a.getContent());
            m.put("createTime", a.getCreateTime());
            result.add(m);
        }
        return Result.success(result);
    }

    // 申请加入社群
    @PostMapping("/community/join")
    public Result<?> joinCommunity(Integer communityId, HttpSession session) {
        SysStudent student = (SysStudent) session.getAttribute("user");
        if (student == null) return Result.error("请先登录");

        SysCommunityMember existRecord = memberMapper.selectOne(new QueryWrapper<SysCommunityMember>()
                .eq("community_id", communityId).eq("student_id", student.getId()));

        if (existRecord != null) {
            if (existRecord.getStatus() == 0) return Result.error("您已提交申请，请等待社长审核");
            if (existRecord.getStatus() == 1) return Result.error("您已经是该社群成员了");
            if (existRecord.getStatus() == 2) return Result.error("您的申请已被拒绝");
        }

        SysCommunityMember member = new SysCommunityMember();
        member.setCommunityId(communityId);
        member.setStudentId(student.getId());
        member.setStatus(0);
        memberMapper.insert(member);

        return Result.success("申请已提交，等待社群管理员审核");
    }
    // 退出社群
    @PostMapping("/community/quit")
    public Result<?> quitCommunity(Integer communityId, HttpSession session) {
        SysStudent student = (SysStudent) session.getAttribute("user");
        if (student == null) return Result.error("请先登录");
        memberMapper.delete(new QueryWrapper<SysCommunityMember>()
                .eq("community_id", communityId).eq("student_id", student.getId()));
        return Result.success("已退出该社群");
    }

    // 查看社群成员列表（学生端）
    @GetMapping("/community/{id}/members")
    public Result<Map<String, Object>> getCommunityMembers(@PathVariable Integer id, HttpSession session) {
        SysStudent student = (SysStudent) session.getAttribute("user");
        if (student == null) return Result.error("请先登录");

        SysCommunity community = communityMapper.selectById(id);
        if (community == null) return Result.error("社群不存在");

        List<SysCommunityMember> members = memberMapper.selectList(new QueryWrapper<SysCommunityMember>()
                .eq("community_id", id).eq("status", 1));
        List<Map<String, Object>> memberList = new ArrayList<>();
        for (SysCommunityMember m : members) {
            SysStudent s = studentMapper.selectById(m.getStudentId());
            if (s == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("studentId", s.getId());
            item.put("studentNo", s.getStudentNo());
            item.put("nickname", s.getNickname());
            item.put("avatar", s.getAvatar());
            item.put("intro", s.getIntro());
            item.put("phone", s.getPhone());
            item.put("email", s.getEmail());
            memberList.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("communityName", community.getName());
        result.put("description", community.getDescription());
        result.put("memberCount", members.size());
        result.put("members", memberList);
        return Result.success(result);
    }

    // 学生申请创建社群
    @PostMapping("/community/create")
    public Result<?> createCommunity(String name, String description, String category, HttpSession session) {
        SysStudent student = (SysStudent) session.getAttribute("user");
        if (student == null) return Result.error("请先登录");
        if (name == null || name.trim().isEmpty()) return Result.error("社群名称不能为空");

        long nameExist = communityMapper.selectCount(new QueryWrapper<SysCommunity>().eq("name", name.trim()));
        if (nameExist > 0) return Result.error("社群名称已存在");

        SysCommunity community = new SysCommunity();
        community.setName(name.trim());
        community.setDescription(description != null ? description.trim() : "");
        community.setCategory(category != null && !category.trim().isEmpty() ? category.trim() : "其他");
        community.setAvatar("https://api.dicebear.com/7.x/bottts/svg?seed=" + System.currentTimeMillis());
        community.setIsRecommended(0);
        community.setStatus(0); // 待审核
        community.setCreatorStudentId(student.getId());
        communityMapper.insert(community);
        return Result.success("社群创建申请已提交，等待平台管理员审核！");
    }

    // 举报社群
    @PostMapping("/community/report")
    public Result<?> reportCommunity(Integer communityId, String reason, HttpSession session) {
        SysStudent student = (SysStudent) session.getAttribute("user");
        if (student == null) return Result.error("请先登录");
        if (reason == null || reason.trim().isEmpty()) return Result.error("举报原因不能为空");
        SysCommunity community = communityMapper.selectById(communityId);
        if (community == null) return Result.error("社群不存在");

        SysReport report = new SysReport();
        report.setReporterId(student.getId());
        report.setCommunityId(communityId);
        report.setReason(reason.trim());
        report.setStatus(0);
        reportMapper.insert(report);
        return Result.success("举报已提交，平台管理员将尽快处理！");
    }
}