package com.hospital.drugmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("drug_info")
public class DrugInfo {
    @TableId(value = "drug_id", type = IdType.AUTO)
    private Long drugId;

    @TableField("drug_code")
    private String drugCode;

    @TableField("drug_name")
    private String drugName;

    @TableField("drug_type")
    private String drugType;

    @TableField("spec")
    private String spec;

    @TableField("unit")
    private String unit;

    @TableField("price")
    private BigDecimal price;

    @TableField("purchase_price")
    private BigDecimal purchasePrice;

    @TableField("warning_num")
    private Integer warningNum;

    @TableField("max_warning_num")
    private Integer maxWarningNum;

    @TableField("shelf_life")
    private Integer shelfLife;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField("production_enterprise")
    private String productionEnterprise;

    @TableField("approval_num")
    private String approvalNum;

    @TableField("status")
    private Integer status;

    // 手动添加 drugId 的 getter/setter
    public Long getDrugId() {
        return drugId;
    }

    public void setDrugId(Long drugId) {
        this.drugId = drugId;
    }

    // 手动添加 drugCode 的 getter/setter（解决61/84行报错）
    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    // 手动添加其他字段的 getter/setter（避免后续报错）
    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getDrugType() {
        return drugType;
    }

    public void setDrugType(String drugType) {
        this.drugType = drugType;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getWarningNum() {
        return warningNum;
    }

    public void setWarningNum(Integer warningNum) {
        this.warningNum = warningNum;
    }

    public Integer getMaxWarningNum() {
        return maxWarningNum;
    }

    public void setMaxWarningNum(Integer maxWarningNum) {
        this.maxWarningNum = maxWarningNum;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Integer getShelfLife() {
        return shelfLife;
    }

    public void setShelfLife(Integer shelfLife) {
        this.shelfLife = shelfLife;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getProductionEnterprise() {
        return productionEnterprise;
    }

    public void setProductionEnterprise(String productionEnterprise) {
        this.productionEnterprise = productionEnterprise;
    }

    public String getApprovalNum() {
        return approvalNum;
    }

    public void setApprovalNum(String approvalNum) {
        this.approvalNum = approvalNum;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}