package com.xysq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

@Data
public class SysComment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer postId;
    private Integer studentId;
    private String content;
    private Integer parentId; // 新增：父评论ID
    private Integer replyToStudentId; // 新增：被回复人ID
    private Date createTime;
}