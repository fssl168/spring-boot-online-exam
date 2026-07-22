/***********************************************************
 * @Description : 注册接口参数
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-16 23:40
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RegisterDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$",
            message = "密码需包含大小写字母和数字，长度 8-64 位")
    private String password;
    @NotBlank(message = "确认密码不能为空")
    private String password2;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3456789]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
    /**
     * 验证码
     */
    private String captcha;
}
