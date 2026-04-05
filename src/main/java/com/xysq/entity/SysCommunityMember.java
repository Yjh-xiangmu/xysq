package com.xysq.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.Date;

@Data
public class SysCommunityMember {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer communityId;
    private Integer studentId;
    private Integer status; // 0待审核, 1已加入, 2已拒绝
    private Date joinTime;
}