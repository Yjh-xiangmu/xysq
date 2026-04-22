package com.xysq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xysq.common.Result;
import com.xysq.entity.*;
import com.xysq.mapper.*;
import com.xysq.mapper.SysFollowMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.ZoneId;
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
    @Autowired private SysFollowMapper followMapper;

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
                                 @RequestParam(value = "scheduledTime", required = false) String scheduledTime,
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

        if (image != null && !image.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                String uploadPath = System.getProperty("user.dir") + "/uploads/posts/";
                File dest = new File(uploadPath + fileName);
                image.transferTo(dest);
                post.setImageUrl("/uploads/posts/" + fileName);
            } catch (Exception e) {
                return Result.error("图片上传失败");
            }
        }

        if (scheduledTime != null && !scheduledTime.trim().isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                Date st = sdf.parse(scheduledTime.trim());
                if (st.after(new Date())) {
                    post.setScheduledTime(st);
                }
            } catch (Exception ignored) {}
        }

        post.setStatus(1);
        postMapper.insert(post);
        return Result.success(post.getScheduledTime() != null ? "定时发布已设置！" : "发布成功！");
    }

    @ResponseBody
    @GetMapping("/api/student/post/list")
    public Result<List<Map<String, Object>>> getPostList(Integer communityId, HttpSession session) {
        Integer currentStudentId = null;
        Object userObj = session.getAttribute("user");
        if (userObj instanceof SysStudent) currentStudentId = ((SysStudent) userObj).getId();

        List<SysPost> posts = postMapper.selectList(new QueryWrapper<SysPost>()
                .eq("community_id", communityId).eq("status", 1)
                .and(w -> w.isNull("scheduled_time").or().le("scheduled_time", new Date()))
                .orderByDesc("create_time"));
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
            Integer signStatus = null;
            if (student != null) {
                SysActivitySign mySign = activitySignMapper.selectOne(new QueryWrapper<SysActivitySign>()
                        .eq("activity_id", act.getId()).eq("student_id", student.getId()));
                if (mySign != null) { isSigned = true; signStatus = mySign.getStatus(); }
            }
            map.put("isSigned", isSigned);
            map.put("signStatus", signStatus);
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

    // 活动报名（填写个人信息，状态为待审核）
    @ResponseBody
    @PostMapping("/api/student/activity/sign")
    public Result<?> signActivity(Integer activityId, String realName, String phone, String remark,
                                  HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent)) return Result.error("请先登录学生账号");
        SysStudent student = (SysStudent) userObj;
        long exist = activitySignMapper.selectCount(new QueryWrapper<SysActivitySign>()
                .eq("activity_id", activityId).eq("student_id", student.getId()));
        if (exist > 0) return Result.error("您已经提交过报名申请了");
        if (realName == null || realName.trim().isEmpty()) return Result.error("请填写真实姓名");
        if (phone == null || phone.trim().isEmpty()) return Result.error("请填写联系电话");
        SysActivitySign sign = new SysActivitySign();
        sign.setActivityId(activityId);
        sign.setStudentId(student.getId());
        sign.setStatus(0); // 待审核
        sign.setRealName(realName.trim());
        sign.setPhone(phone.trim());
        sign.setRemark(remark != null ? remark.trim() : "");
        activitySignMapper.insert(sign);
        return Result.success("报名申请已提交，等待社群管理员审核！");
    }

    // 我评论过的内容
    @ResponseBody
    @GetMapping("/api/student/my-comments")
    public Result<List<Map<String, Object>>> getMyComments(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");

        List<SysComment> comments = commentMapper.selectList(new QueryWrapper<SysComment>()
                .eq("student_id", student.getId()).orderByDesc("create_time"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysComment c : comments) {
            SysPost post = postMapper.selectById(c.getPostId());
            SysCommunity community = post != null ? communityMapper.selectById(post.getCommunityId()) : null;
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("content", c.getContent());
            m.put("createTime", c.getCreateTime());
            m.put("postContent", post != null ? post.getContent() : "帖子已删除");
            m.put("communityName", community != null ? community.getName() : "未知社群");
            m.put("communityId", post != null ? post.getCommunityId() : null);
            result.add(m);
        }
        return Result.success(result);
    }

    // 我赞过的内容
    @ResponseBody
    @GetMapping("/api/student/my-likes")
    public Result<List<Map<String, Object>>> getMyLikes(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");

        List<SysPostLike> likes = postLikeMapper.selectList(new QueryWrapper<SysPostLike>()
                .eq("student_id", student.getId()).orderByDesc("create_time"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysPostLike like : likes) {
            SysPost post = postMapper.selectById(like.getPostId());
            if (post == null) continue;
            SysStudent author = studentMapper.selectById(post.getStudentId());
            SysCommunity community = communityMapper.selectById(post.getCommunityId());
            Map<String, Object> m = new HashMap<>();
            m.put("postId", post.getId());
            m.put("content", post.getContent());
            m.put("imageUrl", post.getImageUrl());
            m.put("createTime", post.getCreateTime());
            m.put("authorName", author != null ? author.getNickname() : "匿名");
            m.put("communityName", community != null ? community.getName() : "未知社群");
            m.put("communityId", post.getCommunityId());
            result.add(m);
        }
        return Result.success(result);
    }

    // 搜索社群内帖子
    @ResponseBody
    @GetMapping("/api/student/community/{id}/search")
    public Result<List<Map<String, Object>>> searchCommunityPosts(@PathVariable("id") Integer communityId,
                                                                   String keyword, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");
        if (keyword == null || keyword.trim().isEmpty()) return Result.success(Collections.emptyList());

        List<SysPost> posts = postMapper.selectList(new QueryWrapper<SysPost>()
                .eq("community_id", communityId).eq("status", 1)
                .like("content", keyword.trim()).orderByDesc("create_time"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysPost p : posts) {
            SysStudent author = studentMapper.selectById(p.getStudentId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("content", p.getContent());
            m.put("imageUrl", p.getImageUrl());
            m.put("createTime", p.getCreateTime());
            m.put("authorName", author != null ? author.getNickname() : "匿名");
            m.put("authorAvatar", author != null ? author.getAvatar() : null);
            result.add(m);
        }
        return Result.success(result);
    }

    @ResponseBody
    @GetMapping("/api/student/activity/all")
    public Result<List<Map<String, Object>>> getAllActivities(HttpSession session) {
        SysStudent student = (SysStudent) session.getAttribute("user");
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000);
        Date now = new Date();

        // 只显示7天内的活动（未来 + 7天内结束的）
        List<SysActivity> activities = activityMapper.selectList(
                new QueryWrapper<SysActivity>().ge("event_time", sevenDaysAgo).orderByAsc("event_time"));

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
            map.put("endTime", act.getEndTime());
            map.put("signCount", signCount);
            map.put("isSigned", isSigned);
            map.put("isExpired", act.getEndTime() != null ? act.getEndTime().before(now) : act.getEventTime() != null && act.getEventTime().before(now));
            map.put("communityName", community != null ? community.getName() : "未知社群");
            result.add(map);
        }
        return Result.success(result);
    }

    // 历史活动：超过7天且学生已参与过的
    @ResponseBody
    @GetMapping("/api/student/activity/history")
    public Result<List<Map<String, Object>>> getActivityHistory(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000);

        List<SysActivitySign> signs = activitySignMapper.selectList(
                new QueryWrapper<SysActivitySign>().eq("student_id", student.getId()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysActivitySign sign : signs) {
            SysActivity act = activityMapper.selectById(sign.getActivityId());
            if (act == null) continue;
            Date effectiveEnd = act.getEndTime() != null ? act.getEndTime() : act.getEventTime();
            if (effectiveEnd == null || !effectiveEnd.before(sevenDaysAgo)) continue;
            SysCommunity community = communityMapper.selectById(act.getCommunityId());
            long signCount = activitySignMapper.selectCount(
                    new QueryWrapper<SysActivitySign>().eq("activity_id", act.getId()));
            Map<String, Object> map = new HashMap<>();
            map.put("id", act.getId());
            map.put("title", act.getTitle());
            map.put("location", act.getLocation());
            map.put("eventTime", act.getEventTime());
            map.put("signCount", signCount);
            map.put("isExpired", true);
            map.put("communityName", community != null ? community.getName() : "未知社群");
            map.put("signStatus", sign.getStatus());
            result.add(map);
        }
        result.sort((a, b) -> ((Date) b.get("eventTime")).compareTo((Date) a.get("eventTime")));
        return Result.success(result);
    }

    // 我的日程：未来且已审核通过的报名活动
    @ResponseBody
    @GetMapping("/api/student/activity/schedule")
    public Result<List<Map<String, Object>>> getMySchedule(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");
        Date now = new Date();

        List<SysActivitySign> signs = activitySignMapper.selectList(
                new QueryWrapper<SysActivitySign>().eq("student_id", student.getId()).eq("status", 1));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysActivitySign sign : signs) {
            SysActivity act = activityMapper.selectById(sign.getActivityId());
            if (act == null) continue;
            Date effectiveEnd = act.getEndTime() != null ? act.getEndTime() : act.getEventTime();
            if (effectiveEnd == null || !effectiveEnd.after(now)) continue;
            SysCommunity community = communityMapper.selectById(act.getCommunityId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", act.getId());
            map.put("title", act.getTitle());
            map.put("location", act.getLocation());
            map.put("eventTime", act.getEventTime());
            map.put("content", act.getContent());
            map.put("communityName", community != null ? community.getName() : "未知社群");
            result.add(map);
        }
        result.sort(Comparator.comparing(m -> (Date) m.get("eventTime")));
        return Result.success(result);
    }

    // 通知列表：评论 + 点赞，自上次已读时间起
    @ResponseBody
    @GetMapping("/api/student/notifications")
    public Result<List<Map<String, Object>>> getNotifications(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");

        Date since = student.getLastNotifRead() != null
                ? student.getLastNotifRead()
                : new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000);

        List<SysPost> myPosts = postMapper.selectList(
                new QueryWrapper<SysPost>().eq("student_id", student.getId()));
        List<Map<String, Object>> result = new ArrayList<>();
        if (myPosts.isEmpty()) return Result.success(result);

        List<Integer> postIds = myPosts.stream().map(SysPost::getId).collect(Collectors.toList());
        Map<Integer, String> postContentMap = new HashMap<>();
        myPosts.forEach(p -> postContentMap.put(p.getId(),
                p.getContent().length() > 20 ? p.getContent().substring(0, 20) + "…" : p.getContent()));

        // 评论通知
        List<SysComment> comments = commentMapper.selectList(new QueryWrapper<SysComment>()
                .in("post_id", postIds).ne("student_id", student.getId()).ge("create_time", since)
                .orderByDesc("create_time"));
        for (SysComment c : comments) {
            SysStudent sender = studentMapper.selectById(c.getStudentId());
            Map<String, Object> m = new HashMap<>();
            m.put("type", "comment");
            m.put("senderName", sender != null ? sender.getNickname() : "有人");
            m.put("senderAvatar", sender != null ? sender.getAvatar() : null);
            m.put("postPreview", postContentMap.getOrDefault(c.getPostId(), "你的帖子"));
            m.put("detail", c.getContent().length() > 30 ? c.getContent().substring(0, 30) + "…" : c.getContent());
            m.put("createTime", c.getCreateTime());
            result.add(m);
        }

        // 点赞通知 (createTime 是 LocalDateTime，需转换比较)
        java.time.LocalDateTime sinceLocal = since.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        List<SysPostLike> likes = postLikeMapper.selectList(new QueryWrapper<SysPostLike>()
                .in("post_id", postIds).ne("student_id", student.getId()).ge("create_time", sinceLocal)
                .orderByDesc("create_time"));
        for (SysPostLike l : likes) {
            SysStudent sender = studentMapper.selectById(l.getStudentId());
            Map<String, Object> m = new HashMap<>();
            m.put("type", "like");
            m.put("senderName", sender != null ? sender.getNickname() : "有人");
            m.put("senderAvatar", sender != null ? sender.getAvatar() : null);
            m.put("postPreview", postContentMap.getOrDefault(l.getPostId(), "你的帖子"));
            m.put("detail", null);
            // 转为 Date 以便统一排序
            m.put("createTime", l.getCreateTime() != null
                    ? Date.from(l.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()) : new Date(0));
            result.add(m);
        }

        result.sort((a, b) -> {
            Date da = a.get("createTime") instanceof Date ? (Date) a.get("createTime") : new Date(0);
            Date db = b.get("createTime") instanceof Date ? (Date) b.get("createTime") : new Date(0);
            return db.compareTo(da);
        });
        return Result.success(result);
    }

    // 标记所有通知已读
    @ResponseBody
    @PostMapping("/api/student/notifications/read")
    public Result<?> markNotificationsRead(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");
        student.setLastNotifRead(new Date());
        studentMapper.updateById(student);
        session.setAttribute("user", student);
        return Result.success("已标记已读");
    }

    // 通知数量：自上次已读时间起未读数
    @ResponseBody
    @GetMapping("/api/student/notifications/count")
    public Result<Map<String, Object>> getNotificationCount(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) {
            Map<String, Object> d = new HashMap<>(); d.put("count", 0); return Result.success(d);
        }
        Date since = student.getLastNotifRead() != null
                ? student.getLastNotifRead()
                : new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000);
        List<SysPost> myPosts = postMapper.selectList(
                new QueryWrapper<SysPost>().eq("student_id", student.getId()));
        if (myPosts.isEmpty()) {
            Map<String, Object> d = new HashMap<>(); d.put("count", 0); return Result.success(d);
        }
        List<Integer> postIds = myPosts.stream().map(SysPost::getId).collect(Collectors.toList());
        long commentCount = commentMapper.selectCount(new QueryWrapper<SysComment>()
                .in("post_id", postIds).ne("student_id", student.getId()).ge("create_time", since));
        long likeCount = postLikeMapper.selectCount(new QueryWrapper<SysPostLike>()
                .in("post_id", postIds).ne("student_id", student.getId()).ge("create_time", since));
        Map<String, Object> data = new HashMap<>();
        data.put("count", commentCount + likeCount);
        return Result.success(data);
    }
}