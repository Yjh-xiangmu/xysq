package com.xysq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

@Data
public class SysReport {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer reporterId;
    private Integer communityId;
    private String reason;
    private Integer status; // 0待审核 1已处理
    private String handleResult;
    private Date createTime;
}
