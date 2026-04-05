package com.xysq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

@Data
public class SysActivitySign {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer activityId;
    private Integer studentId;
    private Date signTime;
}