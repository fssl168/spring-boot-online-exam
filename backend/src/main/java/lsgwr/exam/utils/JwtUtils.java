/***********************************************************
 * @Description : JWT工具类：JWT生产token和校验token的方法
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-21 08:15
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.utils;

import lsgwr.exam.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtUtils {
    /**
     * 构建token的主题
     */
    private static final String SUBJECT = "lsg_exam";
    /**
     * 过期时间：默认 2 小时（B-11 修复：从 24h 缩短为 2h，降低密码修改后旧 token 仍有效的风险窗口）
     * 可由 JwtConfig 通过 setExpire 外部化配置
     */
    private static long EXPIRE = 1000 * 60 * 60 * 2;

    /**
     * JWT 密钥：由 JwtConfig 在启动时从 application.yml 注入
     */
    private static String APP_SECRET = "liangshanguang";

    /**
     * 供 Spring 配置类注入外部化密钥
     */
    public static void setAppSecret(String secret) {
        if (secret != null && !secret.trim().isEmpty()) {
            APP_SECRET = secret;
        }
    }

    /**
     * 供 Spring 配置类注入外部化过期时间（单位：毫秒）
     */
    public static void setExpire(long expire) {
        if (expire > 0) {
            EXPIRE = expire;
        }
    }

    public static String genJsonWebToken(User user) {
        if (user == null || user.getUserId() == null || user.getUserUsername() == null || user.getUserAvatar() == null) {
            return null;
        }
        return Jwts.builder().setSubject(SUBJECT)
                // 下面4行设置token中间字段，携带用户的信息
                .claim("id", user.getUserId())
                .claim("username", user.getUserUsername())
                .claim("avatar", user.getUserAvatar())
                .claim("roleId", user.getUserRoleId())
                .setIssuedAt(new Date())
                // 设置过期时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SignatureAlgorithm.HS256, APP_SECRET)
                // 生成的结果字符串太长，这里压缩下
                .compact();
    }

    /**
     * 校验token
     *
     * @param token 生成的额token
     * @return 解析出的信息
     */
    public static Claims checkJWT(String token) {
        try {
            return Jwts.parser().setSigningKey(APP_SECRET).parseClaimsJws(token).getBody();
        } catch (Exception e) {
            // 篡改token会导致校验失败，走到异常分支，这里返回null
            return null;
        }
    }
}
