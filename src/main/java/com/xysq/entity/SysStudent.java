package com.xysq.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class SysStudent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String studentNo;
    private String password;
    private String nickname;
    private Integer status; // 1正常 0禁用，默认1
}