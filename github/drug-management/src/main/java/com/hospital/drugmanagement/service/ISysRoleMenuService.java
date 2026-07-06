package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.SysRoleMenu;

import java.util.List;

/**
 * 角色菜单关联 Service 接口
 */
public interface ISysRoleMenuService extends IService<SysRoleMenu> {
    /**
     * 根据角色ID获取菜单ID列表
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);
    
    /**
     * 为角色分配菜单
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     */
    void assignMenusToRole(Long roleId, List<Long> menuIds);
}