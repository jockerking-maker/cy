package com.hospital.drugmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("stock_check_item")
public class StockCheckItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long itemId; // 明细ID

    private Long checkId; // 关联盘点单ID

    private Long drugId; // 关联药品ID

    private String batchNo; // 批号（与 drug_stock 批次一一对应）

    private Integer systemNum; // 系统库存数量

    private Integer actualNum; // 实际盘点数量

    private Integer diffNum; // 盈亏数量

    private String handleWay; // 处理方式

    private String handleRemark; // 处理备注
}
