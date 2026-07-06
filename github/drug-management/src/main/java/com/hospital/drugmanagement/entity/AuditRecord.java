package com.hospital.drugmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hospital.drugmanagement.common.anno.AutoFill;
import com.hospital.drugmanagement.common.constant.FillTypeEnum;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("audit_record")
public class AuditRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long recordId; // 记录ID

    private Long orderId; // 关联采购单ID

    private Long auditUserId; // 审核人ID

    private String auditUserName; // 审核人姓名

    private Integer auditResult; // 审核结果（1通过/2驳回）

    private String auditRemark; // 审核意见

    @AutoFill(FillTypeEnum.CREATE_TIME)
    private LocalDateTime auditTime; // 审核时间

    private Integer auditLevel; // 审核级别（1一级/2二级/3三级）
}
