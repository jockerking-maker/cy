package com.hospital.drugmanagement.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.dto.Result;
import com.hospital.drugmanagement.entity.SysRole;
import com.hospital.drugmanagement.entity.SysUser;
import com.hospital.drugmanagement.mapper.SysRoleMapper;
import com.hospital.drugmanagement.mapper.SysUserMapper;
import com.hospital.drugmanagement.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * 全局认证与授权拦截器。
 * <p>
 * 拦截 /api/** 请求：白名单接口（登录、注册、公开信息）直接放行；
 * 其余接口校验 JWT，并结合 {@link com.hospital.drugmanagement.common.anno.RequireRole} 做 RBAC 角色校验。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 无需 Token 即可访问的接口白名单 */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/user/register-roles",
            "/api/public/login-info"
    );

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    /**
     * 请求进入 Controller 前执行：OPTIONS 预检、白名单、JWT 解析、角色校验。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        if (WHITE_LIST.contains(uri)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            writeUnauthorized(response);
            return false;
        }

        String token = authorization;
        if (authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (ExpiredJwtException e) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        } catch (JwtException e) {
            writeUnauthorized(response);
            return false;
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            writeUnauthorized(response, "用户不存在或 token 无效");
            return false;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            writeForbidden(response, "账号已被禁用");
            return false;
        }

        request.setAttribute("currentUserId", userId);

        if (handler instanceof HandlerMethod handlerMethod) {
            RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            if (requireRole == null) {
                requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
            }
            if (requireRole != null) {
                String[] requiredRoles = requireRole.value();
                if (!checkUserRole(user, requiredRoles)) {
                    writeForbidden(response);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean checkUserRole(SysUser user, String[] requiredRoles) {
        if (user.getRoleId() == null) {
            return false;
        }
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        if (role == null) {
            return false;
        }
        String roleCode = role.getRoleCode();
        for (String required : requiredRoles) {
            if (required.equals(roleCode)) {
                return true;
            }
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        writeUnauthorized(response, "未授权，请先登录");
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.error(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void writeForbidden(HttpServletResponse response) throws Exception {
        writeForbidden(response, "权限不足，无法访问该资源");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.error(403, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
