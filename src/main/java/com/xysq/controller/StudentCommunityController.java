package com.xysq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xysq.common.Result;
import com.xysq.entity.SysCommunity;
import com.xysq.entity.SysCommunityMember;
import com.xysq.entity.SysStudent;
import com.xysq.entity.SysAnnouncement;
import com.xysq.mapper.SysAnnouncementMapper;
import com.xysq.mapper.SysCommunityMapper;
import com.xysq.mapper.SysCommunityMemberMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentCommunityController {

    @Autowired private SysCommunityMapper communityMapper;
    @Autowired private SysCommunityMemberMapper memberMapper;
    @Autowired private SysAnnouncementMapper announcementMapper;

    // 获取所有社群列表（广场）- 新增了状态判断
    @GetMapping("/community/list")
    public Result<List<Map<String, Object>>> getCommunityList(HttpSession session) {
        // 获取当前登录的学生
        SysStudent student = (SysStudent) session.getAttribute("user");
        Integer studentId = (student != null) ? student.getId() : null;

        List<SysCommunity> list = communityMapper.selectList(null);
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (SysCommunity c : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("description", c.getDescription());
            map.put("avatar", c.getAvatar());

            // 判断当前学生的加入状态：-1未申请，0审核中，1已加入，2已拒绝
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

    // 获取最新公告（最多3条）
    @GetMapping("/announcements")
    public Result<List<Map<String, Object>>> getAnnouncements() {
        List<SysAnnouncement> list = announcementMapper.selectList(
                new QueryWrapper<SysAnnouncement>().orderByDesc("create_time").last("LIMIT 3"));
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

    // 申请加入社群 (保持不变)
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
}