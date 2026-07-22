/***********************************************************
 * @Description : 主观题批改请求 VO
 ***********************************************************/
package lsgwr.exam.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class EssayGradeVo {
    @NotBlank(message = "考试记录id不能为空")
    private String recordId;
    @NotBlank(message = "题目id不能为空")
    private String questionId;
    @NotNull(message = "评分不能为空")
    private Integer score;
}
