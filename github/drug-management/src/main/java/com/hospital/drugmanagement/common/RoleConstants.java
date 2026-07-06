package com.hospital.drugmanagement.common;

/**
 * 系统角色编码常量，供 {@link com.hospital.drugmanagement.common.anno.RequireRole} 引用。
 */
public final class RoleConstants {

    private RoleConstants() {
    }

    /** 仅系统管理员 */
    public static final String[] ADMIN_ONLY = {"ADMIN"};
    /** 采购相关：管理员 + 采购员 */
    public static final String[] PURCHASE = {"ADMIN", "PURCHASER"};
    /** 审核相关：管理员 + 采购审核员 */
    public static final String[] AUDIT = {"ADMIN", "AUDITOR"};
    /** 仓储相关：管理员 + 库管员 */
    public static final String[] WAREHOUSE = {"ADMIN", "WAREHOUSE"};
}
