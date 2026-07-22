/***********************************************************
 * @Description : 拦截器配置
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-22 08:21
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.config;

import lsgwr.exam.interceptor.LoginInterceptor;
import lsgwr.exam.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class IntercepterConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器：校验token
        registry.addInterceptor(loginInterceptor).addPathPatterns("/api/**");
        // 角色拦截器：校验 @RoleRequired 注解，必须在登录拦截器之后执行
        registry.addInterceptor(roleInterceptor).addPathPatterns("/api/**");
    }

}
