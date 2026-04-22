package com.xysq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

@Data
public class SysMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer fromStudentId;
    private Integer toStudentId;
    private String content;
    private String imageUrl;
    private Integer isRead;
    private Date createTime;
}
