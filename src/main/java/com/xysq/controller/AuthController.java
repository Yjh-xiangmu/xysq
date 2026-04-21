package com.xysq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xysq.common.Result;
import com.xysq.entity.SysAdmin;
import com.xysq.entity.SysStudent;
import com.xysq.mapper.SysAdminMapper;
import com.xysq.mapper.SysStudentMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class AuthController {

    @Autowired private SysAdminMapper adminMapper;
    @Autowired private SysStudentMapper studentMapper;

    @GetMapping("/")
    public String loginPage() { return "login"; }
    @GetMapping("/platform/index")
    public String platformIndex() { return "platform/index"; }
    @GetMapping("/student/index")
    public String studentIndex() { return "student/index"; }
    @GetMapping("/student/profile")
    public String studentProfile() { return "student/profile"; }
    @GetMapping("/student/posts")
    public String studentPosts() { return "student/posts"; }
    @GetMapping("/student/activities")
    public String studentActivities() { return "student/activities"; }
    @GetMapping("/community/member")
    public String communityMember() { return "community/member"; }
    @GetMapping("/community/activity")
    public String communityActivity() { return "community/activity"; }
    @GetMapping("/community/post")
    public String communityPost() { return "community/post-manage"; }
    @GetMapping("/community/info")
    public String communityInfo() { return "community/info"; }
    @GetMapping("/community/profile")
    public String communityProfile() { return "community/profile"; }
    @GetMapping("/community/index")
    public String communityIndex() { return "redirect:/community/member"; }

    @ResponseBody
    @PostMapping("/api/login")
    public Result<?> login(String username, String password, Integer role, HttpSession session) {
        if (role == 1 || role == 2) {
            SysAdmin admin = adminMapper.selectOne(new QueryWrapper<SysAdmin>()
                    .eq("username", username).eq("password", password).eq("role", role));
            if (admin != null) {
                session.setAttribute("user", admin);
                session.setAttribute("role", role);
                return Result.success(role == 1 ? "/platform/index" : "/community/member");
            }
        } else if (role == 3) {
            SysStudent student = studentMapper.selectOne(new QueryWrapper<SysStudent>()
                    .eq("student_no", username).eq("password", password));
            if (student != null) {
                if (student.getStatus() != null && student.getStatus() == 0)
                    return Result.error("账号已被禁用，请联系管理员");

                // 记录登录时间
                student.setLastLoginTime(new Date());
                studentMapper.updateById(student);

                session.setAttribute("user", student);
                session.setAttribute("role", 3);
                return Result.success("/student/index");
            }
        }
        return Result.error("账号或密码错误");
    }

    @ResponseBody
    @PostMapping("/api/register")
    public Result<?> register(String studentNo, String password, String nickname,
                              String intro, String phone, String email,
                              @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) {
        SysStudent exist = studentMapper.selectOne(new QueryWrapper<SysStudent>().eq("student_no", studentNo));
        if (exist != null) return Result.error("学号已存在");

        SysStudent student = new SysStudent();
        student.setStudentNo(studentNo);
        student.setPassword(password);
        student.setNickname(nickname);
        student.setIntro(intro);
        student.setPhone(phone);
        student.setEmail(email);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + avatarFile.getOriginalFilename();
                String uploadPath = System.getProperty("user.dir") + "/uploads/avatars/";
                File dir = new File(uploadPath);
                if (!dir.exists()) dir.mkdirs();
                File dest = new File(dir, fileName);
                avatarFile.transferTo(dest);
                student.setAvatar("/uploads/avatars/" + fileName);
            } catch (Exception e) {
                return Result.error("头像上传失败");
            }
        }
        studentMapper.insert(student);
        return Result.success("注册成功");
    }

    @ResponseBody
    @GetMapping("/api/student/profile")
    public Result<Map<String, Object>> getStudentProfile(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");

        SysStudent current = studentMapper.selectById(student.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("studentNo", current.getStudentNo());
        data.put("nickname", current.getNickname());
        data.put("avatar", current.getAvatar());
        data.put("intro", current.getIntro());
        data.put("phone", current.getPhone());
        data.put("email", current.getEmail());
        return Result.success(data);
    }

    @ResponseBody
    @PostMapping("/api/student/profile/update")
    public Result<?> updateStudentProfile(String nickname, String intro, String phone, String email,
                                          @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                          HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");
        if (nickname == null || nickname.trim().isEmpty()) return Result.error("昵称不能为空");

        SysStudent dbStudent = studentMapper.selectById(student.getId());
        dbStudent.setNickname(nickname.trim());
        dbStudent.setIntro(intro != null ? intro.trim() : "");
        dbStudent.setPhone(phone != null ? phone.trim() : "");
        dbStudent.setEmail(email != null ? email.trim() : "");

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + avatarFile.getOriginalFilename();
                String uploadPath = System.getProperty("user.dir") + "/uploads/avatars/";
                File dir = new File(uploadPath);
                if (!dir.exists()) dir.mkdirs();
                avatarFile.transferTo(new File(dir, fileName));
                dbStudent.setAvatar("/uploads/avatars/" + fileName);
            } catch (Exception e) {
                return Result.error("头像上传失败");
            }
        }
        studentMapper.updateById(dbStudent);
        session.setAttribute("user", dbStudent);
        return Result.success("资料修改成功");
    }

    @ResponseBody
    @PostMapping("/api/student/password/update")
    public Result<?> updateStudentPassword(String oldPwd, String newPwd, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof SysStudent student)) return Result.error("请先登录");
        SysStudent dbStudent = studentMapper.selectById(student.getId());
        if (!dbStudent.getPassword().equals(oldPwd)) return Result.error("原密码错误");
        if (newPwd == null || newPwd.length() < 6) return Result.error("新密码不能少于6位");
        dbStudent.setPassword(newPwd);
        studentMapper.updateById(dbStudent);
        session.invalidate();
        return Result.success("密码修改成功，请重新登录");
    }
    @ResponseBody
    @GetMapping("/api/student/public-profile")
    public Result<Map<String, Object>> getPublicProfile(Integer studentId) {
        SysStudent s = studentMapper.selectById(studentId);
        if (s == null) return Result.error("用户不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", s.getNickname());
        data.put("avatar", s.getAvatar());
        data.put("intro", s.getIntro());
        data.put("phone", s.getPhone());
        data.put("email", s.getEmail());
        return Result.success(data);
    }
}