package lsgwr.exam.util;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Batch 5.3.1 ID 字符串构建与解析工具类
 *
 * 项目中多处使用 "-" / "_" / "$" 作为分隔符拼接 ID 字符串，存在大量重复代码：
 *   - 题目选项 ID：optionId1-optionId2-optionId3
 *   - 考试题目 ID：radioId1-radioId2-radioId3
 *   - 考试记录作答：questionId@True_opt1-opt2$questionId@False_opt3$
 *
 * 本类统一封装"拼接"和"拆分"两个方向的逻辑，消除散落在各处的
 * `+= id + "-"` 和 `str.split("-")` 代码片段。
 */
public final class IdListBuilder {

    /** 默认分隔符：题目选项 ID、考试题目 ID 等 */
    public static final String DEFAULT_SEP = "-";

    /** 题目与选项之间分隔符（用于考试记录作答串） */
    public static final String QUESTION_OPTION_SEP = "_";

    /** 题目之间分隔符（用于考试记录作答串） */
    public static final String QUESTION_SEP = "$";

    private IdListBuilder() {
    }

    // ==================== 拼接 ====================

    /**
     * 将 ID 集合拼接为 "id1-id2-id3" 格式（使用默认分隔符 "-"）
     *
     * @param ids id 集合
     * @return 拼接后的字符串，空集合返回空串
     */
    public static String join(Collection<String> ids) {
        return join(ids, DEFAULT_SEP);
    }

    /**
     * 将 ID 集合按指定分隔符拼接
     *
     * @param ids id 集合
     * @param sep 分隔符
     * @return 拼接后的字符串，空集合返回空串
     */
    public static String join(Collection<String> ids, String sep) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String id : ids) {
            if (StrUtil.isBlank(id)) continue;
            if (!first) {
                sb.append(sep);
            }
            sb.append(id);
            first = false;
        }
        return sb.toString();
    }

    /**
     * 追加单个 ID 到现有 StringBuilder（带分隔符）
     * 用于循环中逐步构建 ID 串，等价于 `sb.append(id).append("-")`
     *
     * @param sb  已有的 StringBuilder
     * @param id  待追加的 id
     * @param sep 分隔符
     */
    public static void appendWithSep(StringBuilder sb, String id, String sep) {
        if (StrUtil.isBlank(id)) return;
        if (sb.length() > 0) {
            sb.append(sep);
        }
        sb.append(id);
    }

    /**
     * 追加单个 ID 到现有 StringBuilder（默认分隔符 "-"）
     */
    public static void appendWithSep(StringBuilder sb, String id) {
        appendWithSep(sb, id, DEFAULT_SEP);
    }

    // ==================== 拆分 ====================

    /**
     * 将 "id1-id2-id3" 格式字符串拆分为 List（保留顺序，过滤空白）
     *
     * @param idStr id 字符串
     * @return 拆分后的 List，输入为空返回空 List
     */
    public static List<String> splitToList(String idStr) {
        return splitToList(idStr, DEFAULT_SEP);
    }

    /**
     * 将字符串按指定分隔符拆分为 List（保留顺序，过滤空白）
     */
    public static List<String> splitToList(String idStr, String sep) {
        if (StrUtil.isBlank(idStr)) {
            return new ArrayList<>();
        }
        String[] arr = idStr.split(java.util.regex.Pattern.quote(sep));
        List<String> result = new ArrayList<>(arr.length);
        for (String s : arr) {
            if (StrUtil.isNotBlank(s)) {
                result.add(s.trim());
            }
        }
        return result;
    }

    /**
     * 将字符串拆分为 Set（去重，无序）
     */
    public static Set<String> splitToSet(String idStr) {
        return splitToSet(idStr, DEFAULT_SEP);
    }

    /**
     * 将字符串按指定分隔符拆分为 Set（去重）
     */
    public static Set<String> splitToSet(String idStr, String sep) {
        return new LinkedHashSet<>(splitToList(idStr, sep));
    }

    /**
     * 将字符串拆分为数组（保留原始 split 语义，仅供兼容老代码使用）
     */
    public static String[] splitToArray(String idStr) {
        return splitToList(idStr).toArray(new String[0]);
    }

    /**
     * 移除字符串末尾的分隔符（如 "id1-id2-" → "id1-id2"）
     * 兼容 "-" / "_" / "$" 三种分隔符
     */
    public static String trimTrailingSep(String str) {
        if (StrUtil.isBlank(str)) return str;
        str = StrUtil.removeSuffix(str, DEFAULT_SEP);
        str = StrUtil.removeSuffix(str, QUESTION_OPTION_SEP);
        str = StrUtil.removeSuffix(str, QUESTION_SEP);
        return str;
    }

    /**
     * 将数组装饰为 Iterable，便于调用 findAllById 等 API
     */
    public static Iterable<String> asIterable(String[] arr) {
        return Arrays.asList(arr == null ? new String[0] : arr);
    }
}
