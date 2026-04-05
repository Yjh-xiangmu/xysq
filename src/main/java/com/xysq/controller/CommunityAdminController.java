package com.xysq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xysq.common.Result;
import com.xysq.entity.SysActivity;
import com.xysq.entity.SysAdmin;
import com.xysq.entity.SysCommunityMember;
import com.xysq.entity.SysStudent;
import com.xysq.mapper.SysActivityMapper;
import com.xysq.mapper.SysCommunityMemberMapper;
import com.xysq.mapper.SysStudentMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

@RestController
@RequestMapping("/api/community")
public class CommunityAdminController {

    @Autowired
    private SysCommunityMemberMapper memberMapper;
    @Autowired
    private SysStudentMapper studentMapper;
    @Autowired
    private SysActivityMapper activityMapper;

    // 获取待审核成员列表
    @GetMapping("/member/auditList")
    public Result<List<Map<String, Object>>> getAuditList(HttpSession session) {
        SysAdmin admin = (SysAdmin) session.getAttribute("user");
        if (admin == null || admin.getRole() != 2) return Result.error("无权限或未登录");

        Integer currentCommunityId = admin.getCommunityId();
        if (currentCommunityId == null) return Result.error("您的账号尚未绑定任何社群");

        List<SysCommunityMember> auditRecords = memberMapper.selectList(
                new QueryWrapper<SysCommunityMember>()
                        .eq("community_id", currentCommunityId)
                        .eq("status", 0)
        );

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (SysCommunityMember record : auditRecords) {
            SysStudent student = studentMapper.selectById(record.getStudentId());
            if (student != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("recordId", record.getId());
                map.put("studentNo", student.getStudentNo());
                map.put("nickname", student.getNickname());
                map.put("joinTime", record.getJoinTime());
                resultList.add(map);
            }
        }
        return Result.success(resultList);
    }

    // 处理审核 (同意或拒绝)
    @PostMapping("/member/audit")
    public Result<?> auditMember(Integer recordId, Integer status, HttpSession session) {
        SysAdmin admin = (SysAdmin) session.getAttribute("user");
        if (admin == null || admin.getRole() != 2) return Result.error("无权限或未登录");

        SysCommunityMember record = memberMapper.selectById(recordId);
        if (record == null) return Result.error("申请记录不存在");

        if (!record.getCommunityId().equals(admin.getCommunityId())) {
            return Result.error("越权操作！您只能审核本社群的申请");
        }

        record.setStatus(status);
        memberMapper.updateById(record);
        return Result.success("审核操作成功");
    }

    // 社长发布新活动 (接收前端 datetime-local 的时间格式)
    @PostMapping("/activity/publish")
    public Result<?> publishActivity(String title, String content, String location,
                                     @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date eventTime,
                                     HttpSession session) {
        SysAdmin admin = (SysAdmin) session.getAttribute("user");
        if (admin == null || admin.getRole() != 2) return Result.error("无权限或未登录");
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
}