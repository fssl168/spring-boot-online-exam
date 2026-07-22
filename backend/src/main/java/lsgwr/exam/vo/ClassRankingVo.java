/***********************************************************
 * @Description : 班级成绩排名 VO
 ***********************************************************/
package lsgwr.exam.vo;

import lombok.Data;

@Data
public class ClassRankingVo {
    /** 排名 */
    private Integer rank;
    /** 学生id */
    private String userId;
    /** 学生昵称 */
    private String nickname;
    /** 考试得分 */
    private Integer score;
    /** 考试耗时（秒） */
    private Integer timeCost;
    /** 参加时间 */
    private String joinDate;
}
