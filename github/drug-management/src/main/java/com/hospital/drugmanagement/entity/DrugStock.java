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
@TableName("drug_stock")
public class DrugStock implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long stockId; // 库存 ID

    private Long drugId; // 关联药品 ID

    private Long warehouseId; // 关联仓库 ID

    @TableField("quantity")
    private Integer stockNum; // 当前库存数量

    @TableField("lock_num")
    private Integer lockNum; // 锁定数量

    private String batchNo; // 批号

    private LocalDateTime productionDate; // 生产日期

    private LocalDateTime expiryDate; // 有效期

    @AutoFill(FillTypeEnum.CREATE_TIME)
    private LocalDateTime createTime; // 创建时间

    @AutoFill(FillTypeEnum.UPDATE_TIME)
    private LocalDateTime updateTime; // 更新时间

    @TableField(exist = false)
    private LocalDateTime lastOperateTime; // 最后操作时间
}