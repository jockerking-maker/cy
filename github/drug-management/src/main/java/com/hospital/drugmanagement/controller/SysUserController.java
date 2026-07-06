package com.hospital.drugmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.dto.LoginRequest;
import com.hospital.drugmanagement.dto.LoginResponse;
import com.hospital.drugmanagement.entity.SysUser;
import com.hospital.drugmanagement.entity.SysRole;
import com.hospital.drugmanagement.entity.SysMenu;
import com.hospital.drugmanagement.service.ISysUserService;
import com.hospital.drugmanagement.service.ISysMenuService;
import com.hospital.drugmanagement.mapper.SysRoleMapper;
import com.hospital.drugmanagement.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统用户接口：登录、注册、用户 CRUD、密码重置。
 * <p>
 * 登录/注册在白名单中无需 Token；用户管理接口需 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class SysUserController {

    @Autowired
    private ISysUserService sysUserService;
    
    @Autowired
    private SysRoleMapper sysRoleMapper;
    
    @Autowired
    private ISysMenuService sysMenuService;

    @Autowired
    private PasswordUtil passwordUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> result = new HashMap<>();
        try {
            LoginResponse response = sysUserService.login(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
            );
            result.put("code", 200);
            result.put("msg", "登录成功");
            result.put("data", response);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
            result.put("data", null);
        } catch (RuntimeException e) {
            result.put("code", 401);
            result.put("msg", e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "登录失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 用户自助注册：仅允许采购员(roleId=3)、库管员(roleId=4)。
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String username = params.get("username") != null ? params.get("username").toString().trim() : null;
            String password = params.get("password") != null ? params.get("password").toString() : null;
            String realName = params.get("realName") != null ? params.get("realName").toString().trim() : null;
            String phone = params.get("phone") != null ? params.get("phone").toString().trim() : null;
            String email = params.get("email") != null ? params.get("email").toString().trim() : null;
            Long roleId = params.get("roleId") != null ? Long.valueOf(params.get("roleId").toString()) : null;

            if (username == null || username.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "用户名不能为空");
                result.put("data", null);
                return result;
            }
            if (username.length() < 3 || username.length() > 20) {
                result.put("code", 400);
                result.put("msg", "用户名长度应为3-20个字符");
                result.put("data", null);
                return result;
            }
            if (password == null || password.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "密码不能为空");
                result.put("data", null);
                return result;
            }
            if (password.length() < 6) {
                result.put("code", 400);
                result.put("msg", "密码长度不能少于6位");
                result.put("data", null);
                return result;
            }
            if (realName == null || realName.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "姓名不能为空");
                result.put("data", null);
                return result;
            }
            if (roleId == null) {
                result.put("code", 400);
                result.put("msg", "请选择角色");
                result.put("data", null);
                return result;
            }
            if (roleId != 3 && roleId != 4) {
                result.put("code", 400);
                result.put("msg", "注册仅支持采购员和库管员角色，其他角色请联系管理员添加");
                result.put("data", null);
                return result;
            }

            LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
            usernameWrapper.eq(SysUser::getUsername, username);
            if (sysUserService.exists(usernameWrapper)) {
                result.put("code", 400);
                result.put("msg", "用户名已存在");
                result.put("data", null);
                return result;
            }

            if (phone != null && !phone.isEmpty()) {
                LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
                phoneWrapper.eq(SysUser::getPhone, phone);
                if (sysUserService.exists(phoneWrapper)) {
                    result.put("code", 400);
                    result.put("msg", "手机号已存在");
                    result.put("data", null);
                    return result;
                }
            }

            if (email != null && !email.isEmpty()) {
                LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
                emailWrapper.eq(SysUser::getEmail, email);
                if (sysUserService.exists(emailWrapper)) {
                    result.put("code", 400);
                    result.put("msg", "邮箱已存在");
                    result.put("data", null);
                    return result;
                }
            }

            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordUtil.encode(password));
            user.setRealName(realName);
            user.setPhone(phone);
            user.setEmail(email);
            user.setRoleId(roleId);
            user.setStatus(1);

            boolean success = sysUserService.save(user);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "注册成功" : "注册失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "注册失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 获取注册页可选角色列表（采购员、库管员）。 */
    @GetMapping("/register-roles")
    public Map<String, Object> getRegisterRoles() {
        Map<String, Object> result = new HashMap<>();
        try {
            LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(SysRole::getRoleId, 3, 4);
            List<SysRole> roles = sysRoleMapper.selectList(queryWrapper);
            List<Map<String, Object>> roleList = new ArrayList<>();
            for (SysRole role : roles) {
                Map<String, Object> map = new HashMap<>();
                map.put("roleId", role.getRoleId());
                map.put("roleName", role.getRoleName());
                map.put("roleCode", role.getRoleCode());
                roleList.add(map);
            }
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", roleList);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败");
            result.put("data", null);
        }
        return result;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Map<String, Object> getCurrentUser(@RequestAttribute("currentUserId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            LoginResponse.UserInfo userInfo = sysUserService.getCurrentUserInfo(userId);
            if (userInfo == null) {
                result.put("code", 404);
                result.put("msg", "用户不存在");
                result.put("data", null);
            } else {
                result.put("code", 200);
                result.put("msg", "success");

                SysUser user = sysUserService.getById(userId);
                List<String> roles;
                if (user != null && user.getRoleId() != null) {
                    SysRole role = sysRoleMapper.selectById(user.getRoleId());
                    if (role != null) {
                        roles = java.util.Arrays.asList(role.getRoleCode());
                    } else {
                        roles = java.util.Arrays.asList("USER");
                    }
                } else {
                    roles = java.util.Arrays.asList("USER");
                }

                Map<String, Object> data = new HashMap<>();
                data.put("userInfo", userInfo);
                data.put("roles", roles);

                List<SysMenu> menus;
                if (user != null && user.getRoleId() != null) {
                    menus = sysMenuService.getMenusByRoleId(user.getRoleId());
                } else {
                    menus = List.of();
                }
                data.put("menus", menus);

                result.put("data", data);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 获取用户列表（分页）
     */
    @GetMapping("/list")
    @RequireRole("ADMIN")
    public Map<String, Object> list(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String realName,
        @RequestParam(required = false) Long roleId
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 构建查询条件
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            if (username != null && !username.isEmpty()) {
                queryWrapper.like(SysUser::getUsername, username);
            }
            if (realName != null && !realName.isEmpty()) {
                queryWrapper.like(SysUser::getRealName, realName);
            }
            if (roleId != null) {
                queryWrapper.eq(SysUser::getRoleId, roleId);
            }
            
            queryWrapper.orderByDesc(SysUser::getCreateTime);

            Page<SysUser> page = sysUserService.page(new Page<>(pageNum, pageSize), queryWrapper);
            List<SysUser> users = page.getRecords();
            
            // 转换用户数据，添加角色名称和处理最后登录时间
            List<Map<String, Object>> userList = new ArrayList<>();
            for (SysUser user : users) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getUserId());
                userMap.put("username", user.getUsername());
                userMap.put("realName", user.getRealName());
                userMap.put("roleId", user.getRoleId());
                userMap.put("phone", user.getPhone());
                userMap.put("email", user.getEmail());
                userMap.put("status", user.getStatus());
                
                // 处理最后登录时间
                if (user.getLastLoginTime() != null) {
                    userMap.put("lastLoginTime", user.getLastLoginTime());
                } else {
                    userMap.put("lastLoginTime", "未登录");
                }
                
                userMap.put("createTime", user.getCreateTime());
                userMap.put("updateTime", user.getUpdateTime());
                
                // 添加角色名称
                if (user.getRoleId() != null) {
                    SysRole role = sysRoleMapper.selectById(user.getRoleId());
                    if (role != null) {
                        userMap.put("roleName", role.getRoleName());
                    } else {
                        userMap.put("roleName", "普通用户");
                    }
                } else {
                    userMap.put("roleName", "普通用户");
                }
                
                userList.add(userMap);
            }
            
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", userList);
            result.put("total", page.getTotal());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 根据 ID 获取用户详情
     */
    @GetMapping("/{id}")
    @RequireRole("ADMIN")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SysUser user = sysUserService.getById(id);
            if (user == null) {
                result.put("code", 404);
                result.put("msg", "用户不存在");
                result.put("data", null);
            } else {
                user.setPassword(null);
                result.put("code", 200);
                result.put("msg", "success");
                result.put("data", user);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 新增用户
     */
    @PostMapping
    @RequireRole("ADMIN")
    public Map<String, Object> save(@RequestBody SysUser user) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 参数校验
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                result.put("code", 400);
                result.put("msg", "用户名不能为空");
                result.put("data", null);
                return result;
            }
            
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                result.put("code", 400);
                result.put("msg", "密码不能为空");
                result.put("data", null);
                return result;
            }
            
            if (user.getRoleId() == null) {
                result.put("code", 400);
                result.put("msg", "角色不能为空");
                result.put("data", null);
                return result;
            }
            
            // 检查用户名是否已存在
            LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
            usernameWrapper.eq(SysUser::getUsername, user.getUsername());
            if (sysUserService.exists(usernameWrapper)) {
                result.put("code", 400);
                result.put("msg", "用户名已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查手机号是否已存在
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
                phoneWrapper.eq(SysUser::getPhone, user.getPhone());
                if (sysUserService.exists(phoneWrapper)) {
                    result.put("code", 400);
                    result.put("msg", "手机号已存在");
                    result.put("data", null);
                    return result;
                }
            }
            
            // 检查邮箱是否已存在
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
                emailWrapper.eq(SysUser::getEmail, user.getEmail());
                if (sysUserService.exists(emailWrapper)) {
                    result.put("code", 400);
                    result.put("msg", "邮箱已存在");
                    result.put("data", null);
                    return result;
                }
            }
            
            String encryptedPassword = passwordUtil.encode(user.getPassword());
            user.setPassword(encryptedPassword);
            
            // 设置默认状态
            if (user.getStatus() == null) {
                user.setStatus(1);
            }
            
            boolean success = sysUserService.save(user);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "保存成功" : "保存失败");
            result.put("data", null);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "保存失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 修改用户
     */
    @PutMapping
    @RequireRole("ADMIN")
    public Map<String, Object> update(@RequestBody SysUser user) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 参数校验
            if (user.getUserId() == null) {
                result.put("code", 400);
                result.put("msg", "用户 ID 不能为空");
                result.put("data", null);
                return result;
            }
            
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                result.put("code", 400);
                result.put("msg", "用户名不能为空");
                result.put("data", null);
                return result;
            }
            
            if (user.getRoleId() == null) {
                result.put("code", 400);
                result.put("msg", "角色不能为空");
                result.put("data", null);
                return result;
            }
            
            // 检查用户名是否已存在（排除当前用户）
            LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
            usernameWrapper.eq(SysUser::getUsername, user.getUsername());
            usernameWrapper.ne(SysUser::getUserId, user.getUserId());
            if (sysUserService.exists(usernameWrapper)) {
                result.put("code", 400);
                result.put("msg", "用户名已存在");
                result.put("data", null);
                return result;
            }
            
            // 检查手机号是否已存在（排除当前用户）
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
                phoneWrapper.eq(SysUser::getPhone, user.getPhone());
                phoneWrapper.ne(SysUser::getUserId, user.getUserId());
                if (sysUserService.exists(phoneWrapper)) {
                    result.put("code", 400);
                    result.put("msg", "手机号已存在");
                    result.put("data", null);
                    return result;
                }
            }
            
            // 检查邮箱是否已存在（排除当前用户）
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
                emailWrapper.eq(SysUser::getEmail, user.getEmail());
                emailWrapper.ne(SysUser::getUserId, user.getUserId());
                if (sysUserService.exists(emailWrapper)) {
                    result.put("code", 400);
                    result.put("msg", "邮箱已存在");
                    result.put("data", null);
                    return result;
                }
            }
            
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordUtil.encode(user.getPassword()));
            } else {
                // 如果密码为空，则不更新密码
                user.setPassword(null);
            }
            
            boolean success = sysUserService.updateById(user);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "更新成功" : "更新失败");
            result.put("data", null);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "更新失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SysUser user = sysUserService.getById(id);
            if (user == null) {
                result.put("code", 404);
                result.put("msg", "用户不存在");
                result.put("data", null);
                return result;
            }
            boolean success = sysUserService.removeById(id);
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
     * 重置密码
     */
    @PutMapping("/resetPassword")
    @RequireRole("ADMIN")
    public Map<String, Object> resetPassword(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String newPassword = request.get("password").toString();

            SysUser user = new SysUser();
            user.setUserId(userId);
            user.setPassword(passwordUtil.encode(newPassword));
            boolean success = sysUserService.updateById(user);
            
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "密码重置成功" : "密码重置失败");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "密码重置失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String realName = (String) request.get("realName");
            String phone = (String) request.get("phone");
            String email = (String) request.get("email");

            if (realName == null || realName.trim().isEmpty()) {
                result.put("code", 400);
                result.put("msg", "姓名不能为空");
                result.put("data", null);
                return result;
            }
            
            // 获取当前用户
            SysUser user = sysUserService.getById(userId);
            if (user == null) {
                result.put("code", 404);
                result.put("msg", "用户不存在");
                result.put("data", null);
                return result;
            }
            
            // 检查手机号是否已存在（排除当前用户）
            if (phone != null && !phone.trim().isEmpty()) {
                LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
                phoneWrapper.eq(SysUser::getPhone, phone.trim());
                phoneWrapper.ne(SysUser::getUserId, userId);
                if (sysUserService.exists(phoneWrapper)) {
                    result.put("code", 400);
                    result.put("msg", "手机号已存在");
                    result.put("data", null);
                    return result;
                }
            }
            
            // 检查邮箱是否已存在（排除当前用户）
            if (email != null && !email.trim().isEmpty()) {
                LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
                emailWrapper.eq(SysUser::getEmail, email.trim());
                emailWrapper.ne(SysUser::getUserId, userId);
                if (sysUserService.exists(emailWrapper)) {
                    result.put("code", 400);
                    result.put("msg", "邮箱已存在");
                    result.put("data", null);
                    return result;
                }
            }
            
            // 更新用户信息
            user.setRealName(realName.trim());
            user.setPhone(phone != null ? phone.trim() : null);
            user.setEmail(email != null ? email.trim() : null);
            
            boolean success = sysUserService.updateById(user);
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "保存成功" : "保存失败");
            result.put("data", null);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "保存失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    public Map<String, Object> changePassword(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String oldPassword = (String) request.get("oldPassword");
            String newPassword = (String) request.get("newPassword");

            if (oldPassword == null || oldPassword.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "原密码不能为空");
                result.put("data", null);
                return result;
            }
            
            if (newPassword == null || newPassword.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "新密码不能为空");
                result.put("data", null);
                return result;
            }
            
            if (newPassword.length() < 6) {
                result.put("code", 400);
                result.put("msg", "密码长度不能少于 6 位");
                result.put("data", null);
                return result;
            }
            
            // 获取当前用户
            SysUser user = sysUserService.getById(userId);
            if (user == null) {
                result.put("code", 404);
                result.put("msg", "用户不存在");
                result.put("data", null);
                return result;
            }
            
            if (!passwordUtil.matches(oldPassword, user.getPassword())) {
                result.put("code", 400);
                result.put("msg", "原密码错误");
                result.put("data", null);
                return result;
            }

            user.setPassword(passwordUtil.encode(newPassword));
            boolean success = sysUserService.updateById(user);
            
            result.put("code", success ? 200 : 500);
            result.put("msg", success ? "密码修改成功" : "密码修改失败");
            result.put("data", null);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "密码修改失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}
