/***********************************************************
 * @Description : I-09/D-06 修复：公告创建/更新 VO，避免 mass assignment
 ***********************************************************/
package lsgwr.exam.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AnnouncementSaveVo {
    /** 公告ID（更新时必填，创建时忽略） */
    private Integer announcementId;
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字符")
    private String title;
    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容最长 10000 字符")
    private String content;
    private String type;
    private Integer pinned;
    private Integer visible;
}
