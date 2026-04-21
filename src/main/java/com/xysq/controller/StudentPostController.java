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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class StudentPostController {

    @Autowired private SysPostMapper postMapper;
    @Autowired private SysCommunityMemberMapper memberMapper;
    @Autowired private SysStudentMapper studentMapper;
    @Autowired private SysCommentMapper commentMapper;
    @Autowired private SysActivityMapper activityMapper;
    @Autowired private SysActivitySignMapper activitySignMapper;
    @Autowired private SysPostLikeMapper postLikeMapper;
    @Autowired private SysCommunityMapper communityMapper;

    @GetMapping("/student/community/{id}")
    public String communityDetail(@PathVariable("id") Integer communityId, Model model, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent)) return "redirect:/";
        SysStudent student = (SysStudent) userObj;
        SysCommunityMember member = memberMapper.selectOne(new QueryWrapper<SysCommunityMember>()
                .eq("community_id", communityId).eq("student_id", student.getId()).eq("status", 1));
        if (member == null) return "redirect:/student/index";
        model.addAttribute("communityId", communityId);
        model.addAttribute("currentStudentId", student.getId());
        return "student/community-detail";
    }

    // 发布帖子：改用 MultipartFile 接收图片
    @ResponseBody
    @PostMapping("/api/student/post/publish")
    public Result<?> publishPost(Integer communityId, String content,
                                 @RequestParam(value = "image", required = false) MultipartFile image,
                                 HttpSession session) {
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

        // 处理图片保存
        if (image != null && !image.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                // 这里的路径就是你刚才创建的文件夹
                String uploadPath = System.getProperty("user.dir") + "/uploads/posts/";
                File dest = new File(uploadPath + fileName);
                image.transferTo(dest);
                // 存入数据库的相对路径
                post.setImageUrl("/uploads/posts/" + fileName);
            } catch (Exception e) {
                return Result.error("图片上传失败");
            }
        }

        post.setStatus(1);
        postMapper.insert(post);
        return Result.success("发布成功！");
    }

    @ResponseBody
    @GetMapping("/api/student/post/list")
    public Result<List<Map<String, Object>>> getPostList(Integer communityId, HttpSession session) {
        Integer currentStudentId = null;
        Object userObj = session.getAttribute("user");
        if (userObj instanceof SysStudent) currentStudentId = ((SysStudent) userObj).getId();

        List<SysPost> posts = postMapper.selectList(new QueryWrapper<SysPost>()
                .eq("community_id", communityId).eq("status", 1).orderByDesc("create_time"));
        if (posts.isEmpty()) return Result.success(Collections.emptyList());

        Set<Integer> studentIds = new HashSet<>();
        List<Integer> postIds = new ArrayList<>();
        for (SysPost p : posts) {
            studentIds.add(p.getStudentId());
            postIds.add(p.getId());
        }

        List<SysComment> allComments = commentMapper.selectList(
                new QueryWrapper<SysComment>().in("post_id", postIds).orderByAsc("create_time"));
        for (SysComment c : allComments) {
            studentIds.add(c.getStudentId());
            if (c.getReplyToStudentId() != null) studentIds.add(c.getReplyToStudentId());
        }

        Map<Integer, String> studentNameMap = new HashMap<>();
        Map<Integer, String> studentAvatarMap = new HashMap<>(); // 存头像
        if (!studentIds.isEmpty()) {
            studentMapper.selectBatchIds(studentIds).forEach(s -> {
                studentNameMap.put(s.getId(), s.getNickname());
                studentAvatarMap.put(s.getId(), s.getAvatar());
            });
        }

        Map<Integer, Long> likeCountMap = new HashMap<>();
        Set<Integer> likedPostIds = new HashSet<>();
        for (Integer pid : postIds) {
            long cnt = postLikeMapper.selectCount(new QueryWrapper<SysPostLike>().eq("post_id", pid));
            likeCountMap.put(pid, cnt);
            if (currentStudentId != null) {
                long my = postLikeMapper.selectCount(new QueryWrapper<SysPostLike>()
                        .eq("post_id", pid).eq("student_id", currentStudentId));
                if (my > 0) likedPostIds.add(pid);
            }
        }

        Map<Integer, List<SysComment>> commentsByPost = allComments.stream()
                .collect(Collectors.groupingBy(SysComment::getPostId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysPost post : posts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("content", post.getContent());
            map.put("imageUrl", post.getImageUrl());
            map.put("createTime", post.getCreateTime());
            map.put("authorName", studentNameMap.getOrDefault(post.getStudentId(), "匿名用户"));
            map.put("authorAvatar", studentAvatarMap.get(post.getStudentId())); // 发帖人头像
            map.put("studentId", post.getStudentId());
            map.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
            map.put("isLiked", likedPostIds.contains(post.getId()));

            List<SysComment> postComments = commentsByPost.getOrDefault(post.getId(), Collections.emptyList());
            map.put("commentCount", postComments.size());

            List<Map<String, Object>> rootComments = new ArrayList<>();
            Map<Integer, Map<String, Object>> rootCommentMap = new HashMap<>();
            for (SysComment c : postComments) {
                if (c.getParentId() == null || c.getParentId() == 0) {
                    Map<String, Object> cMap = new HashMap<>();
                    cMap.put("id", c.getId());
                    cMap.put("studentId", c.getStudentId());
                    cMap.put("content", c.getContent());
                    cMap.put("createTime", c.getCreateTime());
                    cMap.put("authorName", studentNameMap.getOrDefault(c.getStudentId(), "匿名"));
                    cMap.put("authorAvatar", studentAvatarMap.get(c.getStudentId())); // 评论人头像
                    cMap.put("replies", new ArrayList<Map<String, Object>>());
                    rootComments.add(cMap);
                    rootCommentMap.put(c.getId(), cMap);
                }
            }
            for (SysComment c : postComments) {
                if (c.getParentId() != null && c.getParentId() != 0) {
                    Map<String, Object> parentMap = rootCommentMap.get(c.getParentId());
                    if (parentMap != null) {
                        Map<String, Object> replyMap = new HashMap<>();
                        replyMap.put("id", c.getId());
                        replyMap.put("studentId", c.getStudentId());
                        replyMap.put("content", c.getContent());
                        replyMap.put("createTime", c.getCreateTime());
                        replyMap.put("authorName", studentNameMap.getOrDefault(c.getStudentId(), "匿名"));
                        replyMap.put("authorAvatar", studentAvatarMap.get(c.getStudentId())); // 回复人头像
                        replyMap.put("replyToName", studentNameMap.getOrDefault(c.getReplyToStudentId(), "匿名"));
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

    // 后面的点赞、报名、评论等方法不需要修改，直接保留即可
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

    @ResponseBody
    @PostMapping("/api/student/post/like")
    public Result<Map<String, Object>> likePost(Integer postId, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent)) return Result.error("请先登录学生账号");
        SysStudent student = (SysStudent) userObj;

        SysPostLike existing = postLikeMapper.selectOne(new QueryWrapper<SysPostLike>()
                .eq("post_id", postId).eq("student_id", student.getId()));

        boolean nowLiked;
        if (existing != null) {
            postLikeMapper.deleteById(existing.getId());
            nowLiked = false;
        } else {
            SysPostLike like = new SysPostLike();
            like.setPostId(postId);
            like.setStudentId(student.getId());
            postLikeMapper.insert(like);
            nowLiked = true;
        }
        long likeCount = postLikeMapper.selectCount(new QueryWrapper<SysPostLike>().eq("post_id", postId));
        Map<String, Object> data = new HashMap<>();
        data.put("isLiked", nowLiked);
        data.put("likeCount", likeCount);
        return Result.success(data);
    }

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
            long signCount = activitySignMapper.selectCount(
                    new QueryWrapper<SysActivitySign>().eq("activity_id", act.getId()));
            map.put("signCount", signCount);
            boolean isSigned = false;
            if (student != null) {
                isSigned = activitySignMapper.selectCount(new QueryWrapper<SysActivitySign>()
                        .eq("activity_id", act.getId()).eq("student_id", student.getId())) > 0;
            }
            map.put("isSigned", isSigned);
            result.add(map);
        }
        return Result.success(result);
    }

    @ResponseBody
    @GetMapping("/api/student/my-posts")
    public Result<List<Map<String, Object>>> getMyPosts(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");

        List<SysPost> posts = postMapper.selectList(
                new QueryWrapper<SysPost>().eq("student_id", student.getId()).orderByDesc("create_time"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysPost p : posts) {
            SysCommunity community = communityMapper.selectById(p.getCommunityId());
            long commentCount = commentMapper.selectCount(new QueryWrapper<SysComment>().eq("post_id", p.getId()));
            long likeCount = postLikeMapper.selectCount(new QueryWrapper<SysPostLike>().eq("post_id", p.getId()));
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("content", p.getContent());
            m.put("status", p.getStatus());
            m.put("imageUrl", p.getImageUrl());
            m.put("createTime", p.getCreateTime());
            m.put("communityName", community != null ? community.getName() : "已解散社群");
            m.put("communityId", p.getCommunityId());
            m.put("commentCount", commentCount);
            m.put("likeCount", likeCount);
            result.add(m);
        }
        return Result.success(result);
    }

    @ResponseBody
    @PostMapping("/api/student/post/update")
    public Result<?> updatePost(Integer postId, String content, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");
        if (content == null || content.trim().isEmpty()) return Result.error("内容不能为空");
        SysPost post = postMapper.selectById(postId);
        if (post == null) return Result.error("帖子不存在");
        if (!post.getStudentId().equals(student.getId())) return Result.error("只能修改自己的帖子");
        post.setContent(content.trim());
        postMapper.updateById(post);
        return Result.success("修改成功");
    }

    @ResponseBody
    @PostMapping("/api/student/post/delete")
    public Result<?> deletePost(Integer postId, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");
        SysPost post = postMapper.selectById(postId);
        if (post == null) return Result.error("帖子不存在");
        if (!post.getStudentId().equals(student.getId())) return Result.error("只能删除自己的帖子");
        commentMapper.delete(new QueryWrapper<SysComment>().eq("post_id", postId));
        postLikeMapper.delete(new QueryWrapper<SysPostLike>().eq("post_id", postId));
        postMapper.deleteById(postId);
        return Result.success("删除成功");
    }

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

    @ResponseBody
    @GetMapping("/api/student/activity/all")
    public Result<List<Map<String, Object>>> getAllActivities(HttpSession session) {
        SysStudent student = (SysStudent) session.getAttribute("user");

        List<SysActivity> activities = activityMapper.selectList(
                new QueryWrapper<SysActivity>().orderByDesc("event_time"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysActivity act : activities) {
            SysCommunity community = communityMapper.selectById(act.getCommunityId());
            long signCount = activitySignMapper.selectCount(
                    new QueryWrapper<SysActivitySign>().eq("activity_id", act.getId()));
            boolean isSigned = false;
            if (student != null) {
                isSigned = activitySignMapper.selectCount(new QueryWrapper<SysActivitySign>()
                        .eq("activity_id", act.getId()).eq("student_id", student.getId())) > 0;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id", act.getId());
            map.put("title", act.getTitle());
            map.put("content", act.getContent());
            map.put("location", act.getLocation());
            map.put("eventTime", act.getEventTime());
            map.put("signCount", signCount);
            map.put("isSigned", isSigned);
            map.put("communityName", community != null ? community.getName() : "未知社群");
            result.add(map);
        }
        return Result.success(result);
    }
}