/***********************************************************
 * @Description : 参加考试的记录，要有考试记录的id、参与者、参与时间、耗时、得分、得分级别(另建表)
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019/5/14 07:43
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Date;

@Data
@Entity
@Table(name = "exam_record", uniqueConstraints = {
        // P0-2 修复 [D-01, I-02]：同一学生对同一考试只能有一条记录，防止重复交卷污染成绩统计
        @UniqueConstraint(name = "uk_exam_joiner", columnNames = {"exam_id", "exam_joiner_id"})
})
public class ExamRecord {
    /**
     * 主键
     */
    @Id
    private String examRecordId;
    /**
     * 参与的考试的id
     */
    private String examId;

    /**
     * 考生作答地每个题目的选项(题目和题目之间用_分隔，题目有多个选项地话用-分隔),用于查看考试详情
     */
    private String answerOptionIds;

    /**
     * 参与者，即user的id
     */
    private String examJoinerId;
    /**
     * 参加考试的日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date examJoinDate;
    /**
     * 考试耗时(秒)
     */
    private Integer examTimeCost;
    /**
     * 考试得分
     */
    private Integer examJoinScore;
    /**
     * 考试得分水平
     */
    private Integer examResultLevel;
    /**
     * 本次考试展示给考生的题目顺序（按 radio-check-judge 顺序拼接的 dash 分隔字符串）。
     * 由于采用基于 userId 的确定性 shuffle，同一考生每次看到的顺序一致，
     * 此字段作为审计快照，确保即使后续 shuffle 算法变更，历史记录仍可还原原始展示顺序。
     */
    private String questionOrder;
    /**
     * 主观题教师评分，格式：questionId:score,questionId:score
     * NULL 表示无主观题或未批改
     */
    private String essayScores;
    /**
     * S-02 修复：考试记录状态。
     * 0=IN_PROGRESS（开始未提交），1=SUBMITTED（已交卷），2=GRADED（已批改）
     * NULL 视为 SUBMITTED（兼容存量数据）
     */
    private Integer status;
    /**
     * S-04 修复：交卷时考试有效期的审计快照（开始时间）。
     * 即使教师后续修改 examStartDate，历史记录仍可还原原始有效期。
     * NULL 表示存量数据（修复前已存在的记录）。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date examStartDateSnapshot;
    /**
     * S-04 修复：交卷时考试有效期的审计快照（结束时间）。
     * 即使教师后续修改 examEndDate，历史记录仍可还原原始有效期。
     * NULL 表示存量数据（修复前已存在的记录）。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date examEndDateSnapshot;
}
