package com.xysq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class SysAdmin {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    private Integer role; // 1平台管理员 2社群管理员
    private Integer communityId;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Integer studentId; // 学生创群审批后绑定
}