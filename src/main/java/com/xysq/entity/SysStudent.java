package com.xysq.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

@Data
public class SysStudent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String studentNo;
    private String password;
    private String nickname;
    private String avatar;      // 头像路径
    private String intro;       // 个人简介
    private String phone;       // 手机号
    private String email;       // 邮箱
    private Date lastLoginTime; // 最后登录时间
    private Integer status;
}