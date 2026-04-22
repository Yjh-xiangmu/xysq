package com.xysq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xysq.common.Result;
import com.xysq.entity.*;
import com.xysq.mapper.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/student/message")
public class StudentMessageController {

    @Autowired private SysMessageMapper messageMapper;
    @Autowired private SysStudentMapper studentMapper;
    @Autowired private SysFollowMapper followMapper;

    // 获取会话列表（我发或收到的所有人，去重，含最新一条消息）
    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> getConversations(HttpSession session) {
        Object obj = session.getAttribute("user");
        if (!(obj instanceof SysStudent me)) return Result.error("请先登录");

        // 找出所有和我有关的消息，按对话对象分组
        List<SysMessage> sent = messageMapper.selectList(new QueryWrapper<SysMessage>()
                .eq("from_student_id", me.getId()).orderByDesc("create_time"));
        List<SysMessage> received = messageMapper.selectList(new QueryWrapper<SysMessage>()
                .eq("to_student_id", me.getId()).orderByDesc("create_time"));

        // key: 对方studentId, value: 最新消息
        Map<Integer, SysMessage> latestMap = new LinkedHashMap<>();
        for (SysMessage m : sent) {
            latestMap.putIfAbsent(m.getToStudentId(), m);
        }
        for (SysMessage m : received) {
            latestMap.putIfAbsent(m.getFromStudentId(), m);
        }
        // 按时间重排
        List<Map.Entry<Integer, SysMessage>> entries = new ArrayList<>(latestMap.entrySet());
        entries.sort((a, b) -> b.getValue().getCreateTime().compareTo(a.getValue().getCreateTime()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, SysMessage> e : entries) {
            Integer otherId = e.getKey();
            SysStudent other = studentMapper.selectById(otherId);
            if (other == null) continue;
            long unread = messageMapper.selectCount(new QueryWrapper<SysMessage>()
                    .eq("from_student_id", otherId).eq("to_student_id", me.getId()).eq("is_read", 0));
            Map<String, Object> m = new HashMap<>();
            m.put("studentId", otherId);
            m.put("nickname", other.getNickname());
            m.put("avatar", other.getAvatar());
            SysMessage latest = e.getValue();
            m.put("lastContent", latest.getContent() != null ? latest.getContent() : "[图片]");
            m.put("lastTime", latest.getCreateTime());
            m.put("unreadCount", unread);
            result.add(m);
        }
        return Result.success(result);
    }

    // 获取与某人的聊天历史（标记已读）
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getHistory(Integer withId, HttpSession session) {
        Object obj = session.getAttribute("user");
        if (!(obj instanceof SysStudent me)) return Result.error("请先登录");

        // 标记对方发来的消息为已读
        SysMessage update = new SysMessage();
        update.setIsRead(1);
        messageMapper.update(update, new QueryWrapper<SysMessage>()
                .eq("from_student_id", withId).eq("to_student_id", me.getId()).eq("is_read", 0));

        List<SysMessage> messages = messageMapper.selectList(new QueryWrapper<SysMessage>()
                .and(w -> w.eq("from_student_id", me.getId()).eq("to_student_id", withId))
                .or(w -> w.eq("from_student_id", withId).eq("to_student_id", me.getId()))
                .orderByAsc("create_time"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysMessage msg : messages) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", msg.getId());
            m.put("fromStudentId", msg.getFromStudentId());
            m.put("content", msg.getContent());
            m.put("imageUrl", msg.getImageUrl());
            m.put("createTime", msg.getCreateTime());
            m.put("isMine", msg.getFromStudentId().equals(me.getId()));
            result.add(m);
        }
        return Result.success(result);
    }

    // 发送私信（需双方互相关注）
    @PostMapping("/send")
    public Result<?> sendMessage(Integer toStudentId, String content,
                                 @RequestParam(value = "image", required = false) MultipartFile image,
                                 HttpSession session) {
        Object obj = session.getAttribute("user");
        if (!(obj instanceof SysStudent me)) return Result.error("请先登录");
        if (toStudentId == null || toStudentId.equals(me.getId())) return Result.error("发送对象无效");
        if ((content == null || content.trim().isEmpty()) && (image == null || image.isEmpty()))
            return Result.error("消息内容不能为空");

        // 需要互相关注
        boolean iFollow = followMapper.selectCount(new QueryWrapper<SysFollow>()
                .eq("follower_id", me.getId()).eq("followed_id", toStudentId)) > 0;
        boolean theyFollow = followMapper.selectCount(new QueryWrapper<SysFollow>()
                .eq("follower_id", toStudentId).eq("followed_id", me.getId())) > 0;
        if (!iFollow || !theyFollow) return Result.error("需要互相关注才能发送私信");

        SysMessage msg = new SysMessage();
        msg.setFromStudentId(me.getId());
        msg.setToStudentId(toStudentId);
        msg.setContent(content != null ? content.trim() : null);
        msg.setIsRead(0);

        if (image != null && !image.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                String uploadPath = System.getProperty("user.dir") + "/uploads/messages/";
                File dir = new File(uploadPath);
                if (!dir.exists()) dir.mkdirs();
                image.transferTo(new File(dir, fileName));
                msg.setImageUrl("/uploads/messages/" + fileName);
            } catch (Exception e) {
                return Result.error("图片上传失败");
            }
        }

        messageMapper.insert(msg);
        return Result.success("发送成功");
    }

    // 未读消息总数
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount(HttpSession session) {
        Object obj = session.getAttribute("user");
        Map<String, Object> data = new HashMap<>();
        if (!(obj instanceof SysStudent me)) {
            data.put("count", 0);
            return Result.success(data);
        }
        long count = messageMapper.selectCount(new QueryWrapper<SysMessage>()
                .eq("to_student_id", me.getId()).eq("is_read", 0));
        data.put("count", count);
        return Result.success(data);
    }
}
