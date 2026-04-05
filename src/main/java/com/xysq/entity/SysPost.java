package com.xysq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

@Data
public class SysPost {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer communityId;
    private Integer studentId;
    private String content;
    private Integer status; // 0隐藏 1正常
    private Date createTime;
}