package com.hospital.drugmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.SysMenu;
import com.hospital.drugmanagement.entity.SysRole;
import com.hospital.drugmanagement.service.ISysMenuService;
import com.hospital.drugmanagement.service.ISysRoleMenuService;
import com.hospital.drugmanagement.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色与菜单权限接口（RBAC）。
 * <p>
 * 角色 CRUD、菜单树查询、为角色分配菜单权限；仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/api/role")
@CrossOrigin(origins = "*")
@RequireRole("ADMIN")
public class SysRoleController {

    @Autowired
    private ISysRoleService sysRoleService;
    
    @Autowired
    private ISysMenuService sysMenuService;
    
    @Autowired
    private ISysRoleMenuService sysRoleMenuService;

    /**
     * 获取角色列表
     */
    @GetMapping("/list")
    public Map<String, Object> list(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String roleName
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 构建查询条件
            LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
            if (roleName != null && !roleName.isEmpty()) {
                queryWrapper.like(SysRole::getRoleName, roleName);
            }
            
            queryWrapper.orderByAsc(SysRole::getRoleId);

            Page<SysRole> pageResult = sysRoleService.page(new Page<>(pageNum, pageSize), queryWrapper);
            List<SysRole> roles = pageResult.getRecords();

            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", roles);
            result.put("total", pageResult.getTotal());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 根据 ID 获取角色详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SysRole role = sysRoleService.getById(id);
            if (role == null) {
                result.put("code", 404);
                result.put("msg", "角色不存在");
                result.put("data", null);
            } else {
                result.put("code", 200);
                result.put("msg", "success");
                result.put("data", role);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 新增角色
     */
    @PostMapping
    public Map<String, Object> save(@RequestBody SysRole role) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查角色名称是否已存在
            LambdaQueryWrapper<SysRole> roleNameWrapper = new LambdaQueryWrapper<>();
            roleNameWrapper.eq(SysRole::getRoleName, role.getRoleName());
            if (sysRoleService.exists(roleNameWrapper)) {
                result.put("code", 400);
                result.put("msg", "角色名称已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查角色编码是否已存在
            LambdaQueryWrapper<SysRole> roleCodeWrapper = new LambdaQueryWrapper<>();
            roleCodeWrapper.eq(SysRole::getRoleCode, role.getRoleCode());
            if (sysRoleService.exists(roleCodeWrapper)) {
                result.put("code", 400);
                result.put("msg", "角色编码已存在");
                result.put("data", null);
                return result;
            }
            
            boolean success = sysRoleService.save(role);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "保存成功" : "保存失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "保存失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 修改角色
     */
    @PutMapping
    public Map<String, Object> update(@RequestBody SysRole role) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查角色名称是否已存在（排除当前角色）
            LambdaQueryWrapper<SysRole> roleNameWrapper = new LambdaQueryWrapper<>();
            roleNameWrapper.eq(SysRole::getRoleName, role.getRoleName());
            roleNameWrapper.ne(SysRole::getRoleId, role.getRoleId());
            if (sysRoleService.exists(roleNameWrapper)) {
                result.put("code", 400);
                result.put("msg", "角色名称已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查角色编码是否已存在（排除当前角色）
            LambdaQueryWrapper<SysRole> roleCodeWrapper = new LambdaQueryWrapper<>();
            roleCodeWrapper.eq(SysRole::getRoleCode, role.getRoleCode());
            roleCodeWrapper.ne(SysRole::getRoleId, role.getRoleId());
            if (sysRoleService.exists(roleCodeWrapper)) {
                result.put("code", 400);
                result.put("msg", "角色编码已存在");
                result.put("data", null);
                return result;
            }
            
            boolean success = sysRoleService.updateById(role);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "更新成功" : "更新失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "更新失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = sysRoleService.removeById(id);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "删除成功" : "删除失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "删除失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 获取所有菜单
     */
    @GetMapping("/menus")
    public Map<String, Object> getMenus() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<SysMenu> menus = sysMenuService.getAllMenus();
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", menus);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取菜单失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
    
    /**
     * 根据角色ID获取菜单
     */
    @GetMapping("/menus/{roleId}")
    public Map<String, Object> getMenusByRoleId(@PathVariable Long roleId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Long> menuIds = sysRoleMenuService.getMenuIdsByRoleId(roleId);
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", menuIds);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取角色菜单失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
    
    /**
     * 分配权限
     */
    @PostMapping("/assignPerms")
    public Map<String, Object> assignPerms(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查 roleId 是否存在
            if (!request.containsKey("roleId") || request.get("roleId") == null) {
                result.put("code", 400);
                result.put("msg", "角色 ID 不能为空");
                result.put("data", null);
                return result;
            }
            
            Long roleId = Long.valueOf(request.get("roleId").toString());
            List<?> permIds = (List<?>) request.get("permIds");
            
            // 将 permIds 转换为 List<Long>
            List<Long> menuIds = new ArrayList<>();
            if (permIds != null) {
                for (Object id : permIds) {
                    if (id instanceof Integer) {
                        menuIds.add(((Integer) id).longValue());
                    } else if (id instanceof Long) {
                        menuIds.add((Long) id);
                    } else if (id instanceof String) {
                        menuIds.add(Long.valueOf((String) id));
                    }
                }
            }
            
            // 实现权限分配逻辑
            sysRoleMenuService.assignMenusToRole(roleId, menuIds);
            
            result.put("code", 200);
            result.put("msg", "权限分配成功");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "权限分配失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}
