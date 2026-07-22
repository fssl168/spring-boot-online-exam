/***********************************************************
 * @Description : JWT 配置：从 application.yml 读取密钥并注入 JwtUtils
 ***********************************************************/
package lsgwr.exam.config;

import lsgwr.exam.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class JwtConfig {

    @Value("${exam.jwt-secret}")
    private String jwtSecret;

    /**
     * JWT 过期时间（小时），默认 2 小时。B-11 修复：从 24h 缩短为 2h，降低密码修改后旧 token 仍有效的风险窗口。
     */
    @Value("${exam.token-expire-hours:2}")
    private long tokenExpireHours;

    @PostConstruct
    public void init() {
        // A-07 修复：强制校验 JWT 密钥长度 ≥ 32，防止弱密钥导致 token 可被伪造
        if (jwtSecret == null || jwtSecret.trim().length() < 32) {
            throw new IllegalStateException(
                    "JWT 密钥长度不足 32 字符，请通过环境变量 EXAM_JWT_SECRET 配置强密钥。当前长度: "
                            + (jwtSecret == null ? 0 : jwtSecret.trim().length()));
        }
        // 将外部化配置的 JWT 密钥注入到静态工具类
        JwtUtils.setAppSecret(jwtSecret);
        // 将外部化配置的过期时间（小时 -> 毫秒）注入到静态工具类
        JwtUtils.setExpire(tokenExpireHours * 1000 * 60 * 60);
    }
}
