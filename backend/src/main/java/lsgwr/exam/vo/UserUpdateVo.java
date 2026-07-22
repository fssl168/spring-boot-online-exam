/***********************************************************
 * @Description : I-11 修复：用户更新个人信息的VO（仅允许修改非敏感字段）
 ***********************************************************/
package lsgwr.exam.vo;

import lombok.Data;

@Data
public class UserUpdateVo {
    /** 用户昵称 */
    private String userNickname;
    /** 用户头像URL */
    private String userAvatar;
    /** 用户描述 */
    private String userDescription;
    /** 用户邮箱 */
    private String userEmail;
    /** 用户手机号 */
    private String userPhone;
}
