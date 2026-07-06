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
@TableName("purchase_order_item")
public class PurchaseOrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long itemId; // 明细ID

    private Long orderId; // 关联采购单ID

    private Long drugId; // 关联药品ID

    @TableField("purchase_num")
    private Integer purchaseNum; // 采购数量

    private BigDecimal purchasePrice; // 采购单价

    private BigDecimal amount; // 小计金额

    @AutoFill(FillTypeEnum.CREATE_TIME)
    private LocalDateTime createTime; // 创建时间
}