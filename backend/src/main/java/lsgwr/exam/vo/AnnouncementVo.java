/***********************************************************
 * @Description : 公告 VO（Batch 7.3.1）
 *                剥离 creatorId 等内部字段，仅暴露前端需要的展示字段
 * @author      : Batch 7.3.1
 * @date        : 2026-07-23
 ***********************************************************/
package lsgwr.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class AnnouncementVo {
    private Integer announcementId;
    private String title;
    private String content;
    private String type;
    private Integer pinned;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
