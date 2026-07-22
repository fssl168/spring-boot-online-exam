/***********************************************************
 * @Description : 系统公告实体（Batch 7.3.1）
 *                用于首页展示系统通知/公告信息
 * @author      : Batch 7.3.1
 * @date        : 2026-07-23
 ***********************************************************/
package lsgwr.exam.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Data
@DynamicUpdate
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer announcementId;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容（纯文本或简单 HTML）
     */
    private String content;

    /**
     * 公告类型：info/success/warning/error，用于前端图标和颜色区分
     */
    private String type = "info";

    /**
     * 创建者用户ID（管理员）
     */
    private String creatorId;

    /**
     * 是否置顶：1=置顶（按置顶 + 创建时间倒序展示），0=普通
     */
    private Integer pinned = 0;

    /**
     * 是否可见：1=可见（默认），0=隐藏
     * D-05 修复说明：与 Exam/Question 软删除字段保持一致策略，
     * Repository SQL 过滤 `is null or = 1` 兼容存量 NULL 与新数据 1。
     */
    private Integer visible = 1;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
