package com.xysq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xysq.common.Result;
import com.xysq.entity.*;
import com.xysq.mapper.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StudentPostController {

    @Autowired
    private SysPostMapper postMapper;
    @Autowired
    private SysCommunityMemberMapper memberMapper;
    @Autowired
    private SysStudentMapper studentMapper;
    @Autowired
    private SysCommentMapper commentMapper;
    @Autowired
    private SysActivityMapper activityMapper;
    @Autowired
    private SysActivitySignMapper activitySignMapper;

    // 1. 页面跳转
    @GetMapping("/student/community/{id}")
    public String communityDetail(@PathVariable("id") Integer communityId, Model model, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent)) return "redirect:/";

        SysStudent student = (SysStudent) userObj;
        SysCommunityMember member = memberMapper.selectOne(new QueryWrapper<SysCommunityMember>()
                .eq("community_id", communityId).eq("student_id", student.getId()).eq("status", 1));

        if (member == null) return "redirect:/student/index";

        model.addAttribute("communityId", communityId);
        return "student/community-detail";
    }

    // 2. 发布帖子
    @ResponseBody
    @PostMapping("/api/student/post/publish")
    public Result<?> publishPost(Integer communityId, String content, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent)) return Result.error("请先登录学生账号");
        SysStudent student = (SysStudent) userObj;

        SysCommunityMember member = memberMapper.selectOne(new QueryWrapper<SysCommunityMember>()
                .eq("community_id", communityId).eq("student_id", student.getId()).eq("status", 1));
        if (member == null) return Result.error("您不是该社群成员，无法发帖");

        SysPost post = new SysPost();
        post.setCommunityId(communityId);
        post.setStudentId(student.getId());
        post.setContent(content);
        post.setStatus(1);
        postMapper.insert(post);

        return Result.success("发布成功！");
    }

    // 3. 获取帖子列表 (多级评论)
    @ResponseBody
    @GetMapping("/api/student/post/list")
    public Result<List<Map<String, Object>>> getPostList(Integer communityId) {
        List<SysPost> posts = postMapper.selectList(new QueryWrapper<SysPost>()
                .eq("community_id", communityId).eq("status", 1).orderByDesc("create_time"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysPost post : posts) {
            SysStudent author = studentMapper.selectById(post.getStudentId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("content", post.getContent());
            map.put("createTime", post.getCreateTime());
            map.put("authorName", author != null ? author.getNickname() : "匿名用户");

            List<SysComment> allComments = commentMapper.selectList(new QueryWrapper<SysComment>()
                    .eq("post_id", post.getId()).orderByAsc("create_time"));

            List<Map<String, Object>> rootComments = new ArrayList<>();
            Map<Integer, Map<String, Object>> rootCommentMap = new HashMap<>();

            for (SysComment c : allComments) {
                if (c.getParentId() == null || c.getParentId() == 0) {
                    SysStudent cAuthor = studentMapper.selectById(c.getStudentId());
                    Map<String, Object> cMap = new HashMap<>();
                    cMap.put("id", c.getId());
                    cMap.put("studentId", c.getStudentId());
                    cMap.put("content", c.getContent());
                    cMap.put("authorName", cAuthor != null ? cAuthor.getNickname() : "匿名");
                    cMap.put("createTime", c.getCreateTime());
                    cMap.put("replies", new ArrayList<Map<String, Object>>());
                    rootComments.add(cMap);
                    rootCommentMap.put(c.getId(), cMap);
                }
            }

            for (SysComment c : allComments) {
                if (c.getParentId() != null && c.getParentId() != 0) {
                    Map<String, Object> parentMap = rootCommentMap.get(c.getParentId());
                    if (parentMap != null) {
                        SysStudent cAuthor = studentMapper.selectById(c.getStudentId());
                        SysStudent targetStudent = studentMapper.selectById(c.getReplyToStudentId());

                        Map<String, Object> replyMap = new HashMap<>();
                        replyMap.put("id", c.getId());
                        replyMap.put("studentId", c.getStudentId());
                        replyMap.put("content", c.getContent());
                        replyMap.put("authorName", cAuthor != null ? cAuthor.getNickname() : "匿名");
                        replyMap.put("replyToName", targetStudent != null ? targetStudent.getNickname() : "匿名");

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> replies = (List<Map<String, Object>>) parentMap.get("replies");
                        replies.add(replyMap);
                    }
                }
            }
            map.put("comments", rootComments);
            result.add(map);
        }
        return Result.success(result);
    }

    // 4. 发表评论
    @ResponseBody
    @PostMapping("/api/student/post/comment")
    public Result<?> addComment(Integer postId, String content,
                                @RequestParam(defaultValue = "0") Integer parentId,
                                @RequestParam(required = false) Integer replyToStudentId,
                                HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent)) return Result.error("请先登录学生账号");
        SysStudent student = (SysStudent) userObj;

        SysComment comment = new SysComment();
        comment.setPostId(postId);
        comment.setStudentId(student.getId());
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setReplyToStudentId(replyToStudentId);
        commentMapper.insert(comment);
        return Result.success("评论成功！");
    }

    // 5. 获取社群活动列表 (右侧边栏)
    @ResponseBody
    @GetMapping("/api/student/activity/list")
    public Result<List<Map<String, Object>>> getActivityList(Integer communityId, HttpSession session) {
        SysStudent student = (SysStudent) session.getAttribute("user");
        List<SysActivity> activities = activityMapper.selectList(new QueryWrapper<SysActivity>()
                .eq("community_id", communityId).orderByDesc("create_time"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysActivity act : activities) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", act.getId());
            map.put("title", act.getTitle());
            map.put("content", act.getContent());
            map.put("location", act.getLocation());
            map.put("eventTime", act.getEventTime());

            long signCount = activitySignMapper.selectCount(new QueryWrapper<SysActivitySign>().eq("activity_id", act.getId()));
            map.put("signCount", signCount);

            boolean isSigned = false;
            if (student != null) {
                long mySign = activitySignMapper.selectCount(new QueryWrapper<SysActivitySign>()
                        .eq("activity_id", act.getId()).eq("student_id", student.getId()));
                isSigned = mySign > 0;
            }
            map.put("isSigned", isSigned);
            result.add(map);
        }
        return Result.success(result);
    }

    // 6. 学生报名活动
    @ResponseBody
    @PostMapping("/api/student/activity/sign")
    public Result<?> signActivity(Integer activityId, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent)) return Result.error("请先登录学生账号");
        SysStudent student = (SysStudent) userObj;

        long exist = activitySignMapper.selectCount(new QueryWrapper<SysActivitySign>()
                .eq("activity_id", activityId).eq("student_id", student.getId()));
        if (exist > 0) return Result.error("您已经报名过该活动了");

        SysActivitySign sign = new SysActivitySign();
        sign.setActivityId(activityId);
        sign.setStudentId(student.getId());
        activitySignMapper.insert(sign);

        return Result.success("报名成功！");
    }
}