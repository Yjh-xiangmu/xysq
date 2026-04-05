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

@Controller
public class AuthController {

    @Autowired
    private SysAdminMapper adminMapper;
    @Autowired
    private SysStudentMapper studentMapper;

    // ================= 页面路由 =================

    // 登录页
    @GetMapping("/")
    public String loginPage() {
        return "login";
    }

    // 平台管理员首页
    @GetMapping("/platform/index")
    public String platformIndex() {
        return "platform/index";
    }

    // 学生首页 (发现社群广场)
    @GetMapping("/student/index")
    public String studentIndex() {
        return "student/index";
    }

    // 社群管理员 - 成员管理页 (拆分后的新页面)
    @GetMapping("/community/member")
    public String communityMember() {
        return "community/member";
    }

    // 社群管理员 - 活动管理页 (拆分后的新页面)
    @GetMapping("/community/activity")
    public String communityActivity() {
        return "community/activity";
    }

    // 兼容旧代码，防止浏览器有缓存访问原路径报错
    @GetMapping("/community/index")
    public String communityIndex() {
        return "redirect:/community/member";
    }

    // ================= API 接口 =================

    // API：登录接口
    @ResponseBody
    @PostMapping("/api/login")
    public Result<?> login(String username, String password, Integer role, HttpSession session) {
        if (role == 1 || role == 2) {
            SysAdmin admin = adminMapper.selectOne(new QueryWrapper<SysAdmin>()
                    .eq("username", username).eq("password", password).eq("role", role));
            if (admin != null) {
                session.setAttribute("user", admin);
                session.setAttribute("role", role);
                // 角色1跳转平台首页，角色2直接跳转社长成员管理页
                return Result.success(role == 1 ? "/platform/index" : "/community/member");
            }
        } else if (role == 3) {
            SysStudent student = studentMapper.selectOne(new QueryWrapper<SysStudent>()
                    .eq("student_no", username).eq("password", password));
            if (student != null) {
                session.setAttribute("user", student);
                session.setAttribute("role", 3);
                // 角色3跳转学生首页
                return Result.success("/student/index");
            }
        }
        return Result.error("账号或密码错误");
    }

    // API：注册接口 (仅学生)
    @ResponseBody
    @PostMapping("/api/register")
    public Result<?> register(String studentNo, String password, String nickname) {
        SysStudent exist = studentMapper.selectOne(new QueryWrapper<SysStudent>().eq("student_no", studentNo));
        if (exist != null) return Result.error("学号已存在");

        SysStudent student = new SysStudent();
        student.setStudentNo(studentNo);
        student.setPassword(password);
        student.setNickname(nickname);
        studentMapper.insert(student);
        return Result.success("注册成功");
    }
}