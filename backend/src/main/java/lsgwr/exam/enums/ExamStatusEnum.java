package lsgwr.exam.enums;

import lombok.Getter;

/**
 * 考试状态枚举，根据当前时间和考试有效期判断
 */
@Getter
public enum ExamStatusEnum {
    UPCOMING(0, "未开始"),
    AVAILABLE(1, "进行中"),
    ENDED(2, "已结束");

    ExamStatusEnum(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    private Integer id;
    private String description;
}
