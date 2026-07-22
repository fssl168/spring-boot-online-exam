package lsgwr.exam.annotation;

import lsgwr.exam.enums.RoleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色鉴权注解，标注在Controller方法上，指定允许访问的角色
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleRequired {
    /**
     * 允许访问的角色列表
     */
    RoleEnum[] value();
}
