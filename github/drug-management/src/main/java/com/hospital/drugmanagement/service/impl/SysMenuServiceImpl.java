package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.SysMenu;
import com.hospital.drugmanagement.entity.SysRoleMenu;
import com.hospital.drugmanagement.mapper.SysMenuMapper;
import com.hospital.drugmanagement.service.ISysMenuService;
import com.hospital.drugmanagement.service.ISysRoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统菜单业务实现。
 * <p>
 * 提供全部菜单列表，以及按角色 ID 加载该角色可访问的菜单（RBAC 权限菜单树）。
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {
    
    @Autowired
    private ISysRoleMenuService sysRoleMenuService;
    
    /** 查询所有菜单，按 order_num 或 sort 排序。 */
    @Override
    public List<SysMenu> getAllMenus() {
        QueryWrapper<SysMenu> queryWrapper = new QueryWrapper<>();
        // 尝试使用 order_num 字段排序，如果不存在则使用 sort 字段
        try {
            queryWrapper.orderByAsc("order_num");
            return baseMapper.selectList(queryWrapper);
        } catch (Exception e) {
            // 如果 order_num 字段不存在，尝试使用 sort 字段
            QueryWrapper<SysMenu> fallbackQueryWrapper = new QueryWrapper<>();
            fallbackQueryWrapper.orderByAsc("sort");
            return baseMapper.selectList(fallbackQueryWrapper);
        }
    }
    
    /** 根据角色 ID 查询该角色拥有的菜单列表。 */
    @Override
    public List<SysMenu> getMenusByRoleId(Long roleId) {
        List<Long> menuIds = sysRoleMenuService.getMenuIdsByRoleId(roleId);
        if (menuIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper<SysMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("menu_id", menuIds);
        // 尝试使用 order_num 字段排序，如果不存在则使用 sort 字段
        try {
            queryWrapper.orderByAsc("order_num");
            return baseMapper.selectList(queryWrapper);
        } catch (Exception e) {
            // 如果 order_num 字段不存在，尝试使用 sort 字段
            QueryWrapper<SysMenu> fallbackQueryWrapper = new QueryWrapper<>();
            fallbackQueryWrapper.in("menu_id", menuIds);
            fallbackQueryWrapper.orderByAsc("sort");
            return baseMapper.selectList(fallbackQueryWrapper);
        }
    }
}