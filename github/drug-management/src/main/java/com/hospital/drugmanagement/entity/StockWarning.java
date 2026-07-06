package com.hospital.drugmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hospital.drugmanagement.common.anno.AutoFill;
import com.hospital.drugmanagement.common.constant.FillTypeEnum;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("stock_warning")
public class StockWarning implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long warningId; // 预警 ID

    private String warningNo; // 预警单号

    private Long drugId; // 药品 ID

    private Long warehouseId; // 仓库 ID

    private String batchNo; // 批号

    private Integer stockNum; // 当前库存

    private Integer warningType; // 预警类型（0 低于最低预警/1 高于最高预警）

    private Integer minWarningNum; // 最低预警值

    private Integer maxWarningNum; // 最高预警值

    private Integer warningLevel; // 预警级别（0 一般/1 重要/2 紧急）

    private Integer handleStatus; // 处理状态（0 未处理/1 已处理）

    private Long handleUserId; // 处理人 ID

    private LocalDateTime handleTime; // 处理时间

    private String handleRemark; // 处理备注

    @AutoFill(FillTypeEnum.CREATE_TIME)
    private LocalDateTime createTime; // 创建时间

    @AutoFill(FillTypeEnum.UPDATE_TIME)
    private LocalDateTime updateTime; // 更新时间

    @TableField(exist = false)
    private String drugName; // 药品名称（非数据库字段）

    @TableField(exist = false)
    private String warehouseName; // 仓库名称（非数据库字段）

    public Long getWarningId() {
        return warningId;
    }

    public void setWarningId(Long warningId) {
        this.warningId = warningId;
    }

    public String getWarningNo() {
        return warningNo;
    }

    public void setWarningNo(String warningNo) {
        this.warningNo = warningNo;
    }

    public Long getDrugId() {
        return drugId;
    }

    public void setDrugId(Long drugId) {
        this.drugId = drugId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getStockNum() {
        return stockNum;
    }

    public void setStockNum(Integer stockNum) {
        this.stockNum = stockNum;
    }

    public Integer getWarningType() {
        return warningType;
    }

    public void setWarningType(Integer warningType) {
        this.warningType = warningType;
    }

    public Integer getMinWarningNum() {
        return minWarningNum;
    }

    public void setMinWarningNum(Integer minWarningNum) {
        this.minWarningNum = minWarningNum;
    }

    public Integer getMaxWarningNum() {
        return maxWarningNum;
    }

    public void setMaxWarningNum(Integer maxWarningNum) {
        this.maxWarningNum = maxWarningNum;
    }

    public Integer getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(Integer warningLevel) {
        this.warningLevel = warningLevel;
    }

    public Integer getHandleStatus() {
        return handleStatus;
    }

    public void setHandleStatus(Integer handleStatus) {
        this.handleStatus = handleStatus;
    }

    public Long getHandleUserId() {
        return handleUserId;
    }

    public void setHandleUserId(Long handleUserId) {
        this.handleUserId = handleUserId;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }

    public String getHandleRemark() {
        return handleRemark;
    }

    public void setHandleRemark(String handleRemark) {
        this.handleRemark = handleRemark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    private String warningTypeName;

    private Integer daysToExpiry;

    private Long assignUserId;

    private LocalDateTime assignTime;

    private Integer notifyStatus;

    private String notifyChannels;

    private LocalDateTime notifyTime;

    private String suggestion;

    @TableField(exist = false)
    private String handleUserName;

    @TableField(exist = false)
    private String assignUserName;

    @TableField(exist = false)
    private String drugCode;

    public String getWarningTypeName() {
        return warningTypeName;
    }

    public void setWarningTypeName(String warningTypeName) {
        this.warningTypeName = warningTypeName;
    }

    public Integer getDaysToExpiry() {
        return daysToExpiry;
    }

    public void setDaysToExpiry(Integer daysToExpiry) {
        this.daysToExpiry = daysToExpiry;
    }

    public Long getAssignUserId() {
        return assignUserId;
    }

    public void setAssignUserId(Long assignUserId) {
        this.assignUserId = assignUserId;
    }

    public LocalDateTime getAssignTime() {
        return assignTime;
    }

    public void setAssignTime(LocalDateTime assignTime) {
        this.assignTime = assignTime;
    }

    public Integer getNotifyStatus() {
        return notifyStatus;
    }

    public void setNotifyStatus(Integer notifyStatus) {
        this.notifyStatus = notifyStatus;
    }

    public String getNotifyChannels() {
        return notifyChannels;
    }

    public void setNotifyChannels(String notifyChannels) {
        this.notifyChannels = notifyChannels;
    }

    public LocalDateTime getNotifyTime() {
        return notifyTime;
    }

    public void setNotifyTime(LocalDateTime notifyTime) {
        this.notifyTime = notifyTime;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getHandleUserName() {
        return handleUserName;
    }

    public void setHandleUserName(String handleUserName) {
        this.handleUserName = handleUserName;
    }

    public String getAssignUserName() {
        return assignUserName;
    }

    public void setAssignUserName(String assignUserName) {
        this.assignUserName = assignUserName;
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }
}
