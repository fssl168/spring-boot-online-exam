/***********************************************************
 * @Description : 修改密码请求参数
 ***********************************************************/
package lsgwr.exam.qo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ChangePasswordQo {
    /**
     * 旧密码(明文)
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    /**
     * 新密码(明文)
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度需在 8-64 位之间")
    private String newPassword;
}
