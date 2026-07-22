package lsgwr.exam.interceptor;

import lsgwr.exam.annotation.RoleRequired;
import lsgwr.exam.enums.RoleEnum;
import lsgwr.exam.vo.JsonData;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * 角色鉴权拦截器，校验当前用户角色是否满足接口上的 @RoleRequired 注解要求
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 非Controller方法直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 读取方法上的 @RoleRequired 注解，没有注解的接口不校验角色
        RoleRequired roleRequired = handlerMethod.getMethodAnnotation(RoleRequired.class);
        if (roleRequired == null) {
            return true;
        }
        // 从LoginInterceptor中设置的request属性中获取当前用户角色id
        Integer userRoleId = (Integer) request.getAttribute("role_id");
        if (userRoleId == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            sendJsonMessage(response, JsonData.buildError("无法获取用户角色信息，请重新登录"));
            return false;
        }
        // 检查当前用户角色是否在允许的角色列表中
        RoleEnum[] allowedRoles = roleRequired.value();
        boolean allowed = Arrays.stream(allowedRoles)
                .anyMatch(role -> role.getId().equals(userRoleId));
        if (!allowed) {
            // 设置 HTTP 403 状态码，便于前端 axios 统一拦截
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            sendJsonMessage(response, JsonData.buildError("权限不足，无法访问该接口"));
            return false;
        }
        return true;
    }

    /**
     * 响应数据给前端
     */
    public static void sendJsonMessage(HttpServletResponse response, Object obj) throws Exception {
        com.google.gson.Gson g = new com.google.gson.Gson();
        response.setContentType("application/json; charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.print(g.toJson(obj));
        writer.close();
        response.flushBuffer();
    }
}
