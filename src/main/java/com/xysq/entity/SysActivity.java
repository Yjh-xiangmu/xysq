package com.xysq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

@Data
public class SysActivity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer communityId;
    private String title;
    private String content;
    private Date eventTime;
    private String location;
    private Date createTime;
}