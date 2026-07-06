package com.hospital.drugmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统公告实体类
 */
@Data
@TableName("system_notice")
public class SystemNotice implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long noticeId; // 公告 ID

    private String title; // 公告标题

    private String content; // 公告内容

    private Long createUserId; // 创建人 ID

    private String createUserName; // 创建人姓名

    private Integer status; // 状态（0-停用 1-启用）

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间
}
