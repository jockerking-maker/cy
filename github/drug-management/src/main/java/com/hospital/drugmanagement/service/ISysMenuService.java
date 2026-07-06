package com.hospital.drugmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hospital.drugmanagement.entity.SysMenu;

import java.util.List;

/**
 * 系统菜单 Service 接口
 */
public interface ISysMenuService extends IService<SysMenu> {
    /**
     * 获取所有菜单列表
     * @return 菜单列表
     */
    List<SysMenu> getAllMenus();
    
    /**
     * 根据角色ID获取菜单列表
     * @param roleId 角色ID
     * @return 菜单列表
     */
    List<SysMenu> getMenusByRoleId(Long roleId);
}