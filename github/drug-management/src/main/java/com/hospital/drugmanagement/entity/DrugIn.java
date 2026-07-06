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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("drug_in")
public class DrugIn implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long inId; // 入库ID

    private String inNo; // 入库单号

    private Long orderId; // 关联采购单ID

    private Long drugId; // 关联药品ID

    private Long warehouseId; // 入库仓库ID

    private Integer quantity; // 入库数量

    private String batchNo; // 批次号

    private BigDecimal purchasePrice; // 入库单价

    private LocalDate productionDate; // 生产日期

    private LocalDate expiryDate; // 有效期

    private LocalDateTime inDate; // 入库时间

    private Long createUserId; // 操作人 ID

    @TableField("remark")
    private String remark;

    @TableField("in_type")
    private String inType;

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