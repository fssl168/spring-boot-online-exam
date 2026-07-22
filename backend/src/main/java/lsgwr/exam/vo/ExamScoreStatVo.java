package lsgwr.exam.vo;

import lombok.Data;

import java.util.Map;

/**
 * 考试成绩统计VO
 */
@Data
public class ExamScoreStatVo {
    /**
     * 考试id
     */
    private String examId;
    /**
     * 考试名称
     */
    private String examName;
    /**
     * 考试满分
     */
    private Integer examScore;
    /**
     * 参加人数
     */
    private Integer totalCount;
    /**
     * 平均分
     */
    private Double avgScore;
    /**
     * 最高分
     */
    private Integer maxScore;
    /**
     * 最低分
     */
    private Integer minScore;
    /**
     * 及格率（0-100）
     */
    private Double passRate;
    /**
     * 分数段分布，key为分段标签如"90-100"，value为该段人数
     */
    private Map<String, Integer> scoreDistribution;
}
