package com.hospital.drugmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hospital.drugmanagement.common.anno.AutoFill;
import com.hospital.drugmanagement.common.constant.FillTypeEnum;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("drug_out")
public class DrugOut implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long outId; // 出库ID

    private String outNo; // 出库单号

    private Long drugId; // 关联药品ID

    private Long warehouseId; // 出库仓库ID

    private String batchNo; // 批号

    @TableField("quantity")
    private Integer outNum; // 出库数量

    private BigDecimal salePrice; // 出库单价

    private String outType;

    @TableField("relate_no")
    private String relateNo;

    private LocalDateTime outDate; // 出库时间

    private Long createUserId; // 操作人ID

    private String remark; // 备注

    @AutoFill(FillTypeEnum.CREATE_TIME)
    private LocalDateTime createTime; // 创建时间

    // 非数据库字段，用于前端展示
    @TableField(exist = false)
    private String drugName; // 药品名称

    @TableField(exist = false)
    private String spec; // 规格

    @TableField(exist = false)
    private String warehouseName; // 仓库名称

    @TableField(exist = false)
    private String operatorName; // 操作人名称
}