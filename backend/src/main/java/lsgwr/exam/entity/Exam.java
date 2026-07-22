/***********************************************************
 * @Description : 考试表，要有题目、总分数、时间限制、有效日期、创建者等字段
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019/5/14 07:42
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/

package lsgwr.exam.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Data
@DynamicUpdate
public class Exam {
    @Id
    private String examId;
    private String examName;
    private String examAvatar;
    private String examDescription;
    /**
     * D-02 修复说明：此字段为冗余字段，存储 radio-check-judge 三类题目 ID 的拼接结果。
     * 业务逻辑（判分、详情展示、统计）均使用 examQuestionIdsRadio/Check/Judge 分别读取，
     * 此字段仅在 create()/update() 时由三者拼接写入，从未被读取。
     * 保留此字段以兼容存量数据库结构与可能的历史前端消费方，不再扩展用途。
     * 若后续确认无外部依赖，可通过 DB 迁移脚本删除此列。
     */
    private String examQuestionIds;
    private String examQuestionIdsRadio;
    private String examQuestionIdsCheck;
    private String examQuestionIdsJudge;
    private Integer examScore;
    private Integer examScoreRadio;
    private Integer examScoreCheck;
    private Integer examScoreJudge;
    private String examCreatorId;
    private Integer examTimeLimit;
    /**
     * 软删除标记：1=可见(默认)，0=已删除。
     * D-05 修复说明：JPA 新增此列时存量行值为 NULL。
     * - Entity 默认值 `= 1` 保证经 JPA 创建的新记录一定非空
     * - Repository 的 SQL 过滤条件 `is null or = 1` 兼容存量 NULL 与新数据 1，二者配合使用为正确设计
     * - 若未来通过 DB 迁移脚本将所有 NULL 统一更新为 1，可简化 SQL 过滤为 `= 1`
     */
    private Integer examVisible = 1;
    /**
     * S-01 修复：手动状态覆盖。
     * null/0：自动计算（基于 examStartDate/examEndDate）
     * 1：手动结束（强制 ENDED，教师提前收卷）
     * 2：手动暂停（强制不可用，教师暂停考试）
     */
    private Integer examManualStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date examStartDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date examEndDate;
    /**
     * 创建时间。D-03 修复：Service 代码中显式 setCreateTime(new Date())，
     * 并非由数据库自动维护，此注释已更正。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间。D-03 修复：Service 代码中显式 setUpdateTime(new Date())，
     * 并非由数据库自动维护，此注释已更正。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
