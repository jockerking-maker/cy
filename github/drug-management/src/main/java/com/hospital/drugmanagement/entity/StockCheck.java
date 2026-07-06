package com.hospital.drugmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hospital.drugmanagement.common.anno.AutoFill;
import com.hospital.drugmanagement.common.constant.FillTypeEnum;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("stock_check")
public class StockCheck implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long checkId; // 盘点ID

    private String checkNo; // 盘点单号

    private Long warehouseId; // 盘点仓库ID

    @TableField("check_time")
    private LocalDateTime checkTime;

    private Integer status;

    private String remark;

    @TableField("check_user_id")
    private Long checkUserId;

    @AutoFill(FillTypeEnum.CREATE_TIME)
    private LocalDateTime createTime; // 创建时间

    @AutoFill(FillTypeEnum.UPDATE_TIME)
    private LocalDateTime updateTime; // 更新时间
}