package com.hospital.drugmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hospital.drugmanagement.common.anno.AutoFill;
import com.hospital.drugmanagement.common.constant.FillTypeEnum;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("purchase_order")
public class PurchaseOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long orderId; // 采购单ID

    private String orderNo; // 采购单号

    private Long supplierId; // 关联供应商ID

    private LocalDateTime orderDate; // 订单日期

    private BigDecimal totalAmount; // 采购总金额

    private Integer status; // 状态（0待审核/1已审核/2已入库/3已取消）

    private String remark; // 备注

    private Long createUserId; // 创建人ID

    @AutoFill(FillTypeEnum.CREATE_TIME)
    private LocalDateTime createTime; // 创建时间

    @AutoFill(FillTypeEnum.UPDATE_TIME)
    private LocalDateTime updateTime; // 更新时间
}