/***********************************************************
 * @Description : 随机组卷请求 VO
 ***********************************************************/
package lsgwr.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
public class RandomExamCreateVo {

    @NotBlank(message = "考试名称不能为空")
    @JsonProperty("name")
    private String examName;

    @JsonProperty("avatar")
    private String examAvatar;

    @JsonProperty("desc")
    private String examDescription;

    @JsonProperty("elapse")
    private Integer examTimeLimit;

    @JsonProperty("startDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date examStartDate;

    @JsonProperty("endDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date examEndDate;

    /** 单选题数量 */
    @Min(0)
    @JsonProperty("radioCount")
    private Integer radioCount = 0;

    /** 多选题数量 */
    @Min(0)
    @JsonProperty("checkCount")
    private Integer checkCount = 0;

    /** 判断题数量 */
    @Min(0)
    @JsonProperty("judgeCount")
    private Integer judgeCount = 0;

    /** 单选题每题分数 */
    @JsonProperty("radioScore")
    private Integer examScoreRadio = 2;

    /** 多选题每题分数 */
    @JsonProperty("checkScore")
    private Integer examScoreCheck = 4;

    /** 判断题每题分数 */
    @JsonProperty("judgeScore")
    private Integer examScoreJudge = 2;
}
