package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.SysRoleMenu;
import com.hospital.drugmanagement.mapper.SysRoleMenuMapper;
import com.hospital.drugmanagement.service.ISysRoleMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色-菜单关联业务实现。
 * <p>
 * 维护 RBAC 中角色与菜单的多对多关系；分配菜单时先删后增，保证数据一致。
 */
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements ISysRoleMenuService {
    
    /** 查询某角色已分配的菜单 ID 列表。 */
    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        QueryWrapper<SysRoleMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        return baseMapper.selectList(queryWrapper).stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }
    
    /** 为角色重新分配菜单：先清除旧关联，再批量插入新关联。 */
    @Transactional
    @Override
    public void assignMenusToRole(Long roleId, List<Long> menuIds) {
        // 先删除旧的关联
        QueryWrapper<SysRoleMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        baseMapper.delete(queryWrapper);
        
        // 添加新的关联
        if (menuIds != null && !menuIds.isEmpty()) {
            // 去重，避免重复的菜单 ID
            List<Long> uniqueMenuIds = menuIds.stream().distinct().toList();
            
            List<SysRoleMenu> roleMenus = uniqueMenuIds.stream()
                    .map(menuId -> {
                        SysRoleMenu roleMenu = new SysRoleMenu();
                        roleMenu.setRoleId(roleId);
                        roleMenu.setMenuId(menuId);
                        return roleMenu;
                    })
                    .toList();
            saveBatch(roleMenus);
        }
    }
}