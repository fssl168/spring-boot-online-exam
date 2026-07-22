/***********************************************************
 * @Description : 考试卡片列表
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-06-23 19:30
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class ExamCardVo {
    @JsonProperty("id")
    private String examId;
    @JsonProperty("title")
    private String examName;
    @JsonProperty("avatar")
    private String examAvatar;
    @JsonProperty("content")
    private String examDescription;
    @JsonProperty("score")
    private Integer examScore;
    /**
     * 考试限制的时间，单位为分钟
     */
    @JsonProperty("elapse")
    private Integer examTimeLimit;
    /**
     * 考试状态：0-未开始 1-进行中 2-已结束
     */
    @JsonProperty("status")
    private Integer status;
    @JsonProperty("startDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date examStartDate;
    @JsonProperty("endDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date examEndDate;
}
