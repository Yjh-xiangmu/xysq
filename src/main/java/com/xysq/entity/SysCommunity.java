package com.xysq.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

@Data
public class SysCommunity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String description;
    private String avatar;
    private String category;
    private Integer isRecommended;
    private Date createTime;
    private Integer status; // 0待审核 1正常 2已拒绝
    private Integer creatorStudentId;
}