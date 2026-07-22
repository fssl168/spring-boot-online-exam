/***********************************************************
 * @Description : 登录的查询参数
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-19 20:18
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.qo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginQo {
    /**
     * 1表示用户名，2表示邮箱
     */
    @NotNull(message = "登录类型不能为空")
    private Integer loginType;
    /**
     * 用户名/邮箱的字符串
     */
    @NotBlank(message = "用户名/邮箱不能为空")
    private String userInfo;
    /**
     * 用户密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
