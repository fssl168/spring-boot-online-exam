package lsgwr.exam.util;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch 5.3.4 考试作答字符串解析工具类
 *
 * 考试记录中作答串格式较为复杂，散落在 judge() 和 getRecordDetail() 中：
 *   完整串：questionId1@True_opt1-opt2$questionId2@False_opt3$
 *   分隔层级：
 *     - "$" 分隔题目
 *     - "_" 分隔题目标题和选项
 *     - "@" 分隔题目 ID 和判分结果（True/False）
 *     - "-" 分隔多个选项
 *
 * 本类封装两个方向的转换：
 *   1. build：把 Map<questionId, AnswerEntry> 拼接为完整串
 *   2. parse：把完整串解析为 Map<questionId, AnswerEntry>
 */
public final class AnswerParser {

    private AnswerParser() {
    }

    /**
     * 题目作答条目
     */
    public static class AnswerEntry {
        /** 题目 ID */
        private final String questionId;
        /** 是否答对 */
        private final boolean correct;
        /** 用户作答的选项 ID 列表（保留顺序） */
        private final List<String> userOptionIds;

        public AnswerEntry(String questionId, boolean correct, List<String> userOptionIds) {
            this.questionId = questionId;
            this.correct = correct;
            this.userOptionIds = userOptionIds != null ? new ArrayList<>(userOptionIds) : new ArrayList<>();
        }

        public String getQuestionId() { return questionId; }
        public boolean isCorrect() { return correct; }
        public List<String> getUserOptionIds() { return userOptionIds; }
    }

    /**
     * 将单个题目的作答拼接为片段：questionId@True_opt1-opt2
     *
     * @param questionId 题目 ID
     * @param correct    是否答对
     * @param optionIds  用户选项 ID 列表
     * @return 片段字符串
     */
    public static String buildEntry(String questionId, boolean correct, List<String> optionIds) {
        StringBuilder sb = new StringBuilder();
        sb.append(questionId).append("@").append(correct ? "True" : "False").append("_");
        sb.append(IdListBuilder.join(optionIds, IdListBuilder.DEFAULT_SEP));
        return sb.toString();
    }

    /**
     * 将多条作答条目拼接为完整串：entry1$entry2$entry3
     *
     * @param entries 作答条目列表
     * @return 完整串（已去除末尾分隔符）
     */
    public static String build(List<AnswerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (AnswerEntry entry : entries) {
            if (sb.length() > 0) {
                sb.append(IdListBuilder.QUESTION_SEP);
            }
            sb.append(buildEntry(entry.getQuestionId(), entry.isCorrect(), entry.getUserOptionIds()));
        }
        return sb.toString();
    }

    /**
     * 解析完整作答串为 Map<questionId, AnswerEntry>
     *
     * @param answerStr 完整作答串，如 "qid1@True_opt1-opt2$qid2@False_opt3"
     * @return 解析结果，输入为空返回空 Map
     */
    public static Map<String, AnswerEntry> parse(String answerStr) {
        Map<String, AnswerEntry> result = new LinkedHashMap<>();
        if (StrUtil.isBlank(answerStr)) {
            return result;
        }
        // "$" 在正则中是行尾符，需 quote 或使用 Pattern.quote
        String[] entries = answerStr.split("[" + IdListBuilder.QUESTION_SEP + "]");
        for (String entry : entries) {
            if (StrUtil.isBlank(entry)) continue;
            // 拆分题目信息和选项：qid@True_opt1-opt2
            String[] titleAndOptions = entry.split(IdListBuilder.QUESTION_OPTION_SEP);
            if (titleAndOptions.length < 2) continue;
            // 拆分题目 ID 和判分结果：qid@True
            String[] idAndResult = titleAndOptions[0].split("@");
            if (idAndResult.length < 2) continue;
            String questionId = idAndResult[0];
            boolean correct = "True".equalsIgnoreCase(idAndResult[1]);
            List<String> optionIds = IdListBuilder.splitToList(titleAndOptions[1], IdListBuilder.DEFAULT_SEP);
            result.put(questionId, new AnswerEntry(questionId, correct, optionIds));
        }
        return result;
    }

    /**
     * 从作答串中提取用户作答 Map（兼容老接口 HashMap<String, List<String>>）
     */
    public static Map<String, List<String>> parseUserAnswers(String answerStr) {
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, AnswerEntry> e : parse(answerStr).entrySet()) {
            result.put(e.getKey(), e.getValue().getUserOptionIds());
        }
        return result;
    }

    /**
     * 从作答串中提取判分结果 Map：questionId → "True"/"False"
     */
    public static Map<String, String> parseResults(String answerStr) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, AnswerEntry> e : parse(answerStr).entrySet()) {
            result.put(e.getKey(), e.getValue().isCorrect() ? "True" : "False");
        }
        return result;
    }

    /**
     * 对选项 ID 列表排序后拼接为字符串，用于比较答案是否一致
     */
    public static String sortedJoin(List<String> optionIds) {
        List<String> sorted = new ArrayList<>(optionIds);
        Collections.sort(sorted);
        return IdListBuilder.join(sorted, IdListBuilder.DEFAULT_SEP);
    }

    /**
     * 比较两个选项列表是否一致（排序后比较）
     */
    public static boolean optionsEquals(List<String> a, List<String> b) {
        return sortedJoin(a).equals(sortedJoin(b));
    }
}
