package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.dto.LoginResponse;
import com.hospital.drugmanagement.entity.SysMenu;
import com.hospital.drugmanagement.entity.SysRole;
import com.hospital.drugmanagement.entity.SysUser;
import com.hospital.drugmanagement.mapper.SysRoleMapper;
import com.hospital.drugmanagement.mapper.SysUserMapper;
import com.hospital.drugmanagement.service.ISysMenuService;
import com.hospital.drugmanagement.service.ISysUserService;
import com.hospital.drugmanagement.util.JwtUtil;
import com.hospital.drugmanagement.util.PasswordUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 系统用户业务：登录鉴权、角色菜单加载、密码自动升级（MD5 → BCrypt）。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private ISysMenuService sysMenuService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordUtil passwordUtil;

    /**
     * 登录：校验账号密码 → 生成 JWT → 返回用户信息与角色菜单。
     * 若检测到旧版 MD5 密码，登录成功后自动迁移为 BCrypt。
     */
    @Override
    public LoginResponse login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordUtil.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        if (passwordUtil.isLegacyPassword(user.getPassword())) {
            user.setPassword(passwordUtil.encode(password));
        }

        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtil.generateToken(user.getUserId(), user.getUsername()));

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        response.setUserInfo(userInfo);

        if (user.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(user.getRoleId());
            if (role != null) {
                response.setRoles(Arrays.asList(role.getRoleCode()));
                List<SysMenu> menus = sysMenuService.getMenusByRoleId(user.getRoleId());
                response.setMenus(menus);
            } else {
                response.setRoles(Arrays.asList("USER"));
                response.setMenus(Arrays.asList());
            }
        } else {
            response.setRoles(Arrays.asList("USER"));
            response.setMenus(Arrays.asList());
        }

        return response;
    }

    /** 根据用户 ID 返回当前登录用户基本信息（不含 Token）。 */
    @Override
    public LoginResponse.UserInfo getCurrentUserInfo(Long userId) {
        if (userId == null) {
            return null;
        }

        SysUser user = this.getById(userId);
        if (user == null) {
            return null;
        }

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        return userInfo;
    }
}
