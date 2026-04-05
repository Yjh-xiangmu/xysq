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
    private Integer communityId; // 新增：该社长管理的社群ID
}