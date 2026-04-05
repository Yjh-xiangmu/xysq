package com.xysq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_post_like")
public class SysPostLike {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer postId;
    private Integer studentId;
    private LocalDateTime createTime;
}
