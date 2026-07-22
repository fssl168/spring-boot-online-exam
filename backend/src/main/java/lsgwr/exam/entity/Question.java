/***********************************************************
 * @Description : 考试题目表
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019/5/14 07:46
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Data
@Entity
@DynamicUpdate
public class Question {
    @Id
    private String questionId;
    private String questionName;
    private Integer questionScore;
    private String questionCreatorId;
    private Integer questionLevelId;
    private Integer questionTypeId;
    private Integer questionCategoryId;
    private String questionDescription;
    private String questionOptionIds;
    private String questionAnswerOptionIds;
    /**
     * 软删除标记：1=可见(默认)，0=已删除。
     * D-05 修复说明：JPA 新增此列时存量行值为 NULL。
     * - Entity 默认值 `= 1` 保证经 JPA 创建的新记录一定非空
     * - Repository 的 SQL 过滤条件 `is null or = 1` 兼容存量 NULL 与新数据 1，二者配合使用为正确设计
     * - 若未来通过 DB 迁移脚本将所有 NULL 统一更新为 1，可简化 SQL 过滤为 `= 1`
     */
    private Integer questionVisible = 1;
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
