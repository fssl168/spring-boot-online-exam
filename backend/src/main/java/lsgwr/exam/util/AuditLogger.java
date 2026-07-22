/***********************************************************
 * @Description : P-08 修复：统一审计日志工具
 *                使用独立的 "audit" Logger，可在 logback 配置中
 *                单独输出到 audit.log 文件，便于合规审计与排查。
 *                审计日志格式：[AUDIT] actor=<userId> action=<action>
 *                              target=<targetId> result=<result> detail=<detail>
 * @author      : P-08
 * @date        : 2026-07-23
 ***********************************************************/
package lsgwr.exam.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 审计日志工具类
 * <p>
 * 使用方式：
 * <pre>
 * AuditLogger.log("user123", "EXAM_DELETE", "exam_456", "SUCCESS", "教师删除考试");
 * AuditLogger.log("user123", "LOGIN", null, "FAILURE", "密码错误");
 * </pre>
 * <p>
 * 推荐在 logback-spring.xml 中为 "audit" logger 单独配置 appender，
 * 输出到 logs/audit.log，并设置保留策略（如保留 90 天）。
 */
public final class AuditLogger {
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("audit");

    private AuditLogger() {
    }

    /**
     * 记录审计日志
     *
     * @param actor  操作人（userId 或 "anonymous"）
     * @param action 操作类型（如 LOGIN, EXAM_CREATE, PASSWORD_CHANGE 等）
     * @param target 操作目标 ID（可为 null）
     * @param result 操作结果（SUCCESS / FAILURE / DENIED）
     * @param detail 详细信息（可为 null）
     */
    public static void log(String actor, String action, String target, String result, String detail) {
        if (AUDIT_LOGGER.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder(64);
            sb.append("[AUDIT] actor=").append(safe(actor));
            sb.append(" action=").append(safe(action));
            sb.append(" target=").append(target != null ? safe(target) : "-");
            sb.append(" result=").append(safe(result));
            if (detail != null && !detail.isEmpty()) {
                sb.append(" detail=").append(safe(detail));
            }
            AUDIT_LOGGER.info(sb.toString());
        }
    }

    /**
     * 记录成功操作
     */
    public static void success(String actor, String action, String target, String detail) {
        log(actor, action, target, "SUCCESS", detail);
    }

    /**
     * 记录失败操作
     */
    public static void failure(String actor, String action, String target, String detail) {
        log(actor, action, target, "FAILURE", detail);
    }

    /**
     * 记录拒绝操作（权限不足）
     */
    public static void denied(String actor, String action, String target, String detail) {
        log(actor, action, target, "DENIED", detail);
    }

    /**
     * 安全转义：移除换行符，防止日志注入
     */
    private static String safe(String value) {
        if (value == null) return "-";
        return value.replace('\r', '_').replace('\n', '_').replace('\t', '_');
    }
}
