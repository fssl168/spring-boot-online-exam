/***********************************************************
 * @Description : 对外REST接口
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-16 23:45
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.controller;

import lsgwr.exam.annotation.RoleRequired;
import lsgwr.exam.component.LoginAttemptService;
import lsgwr.exam.dto.RegisterDTO;
import lsgwr.exam.entity.User;
import lsgwr.exam.enums.ResultEnum;
import lsgwr.exam.enums.RoleEnum;
import lsgwr.exam.exception.ExamException;
import lsgwr.exam.qo.ChangePasswordQo;
import lsgwr.exam.qo.LoginQo;
import lsgwr.exam.service.UserService;
import lsgwr.exam.vo.ResultVO;
import lsgwr.exam.vo.UserInfoVo;
import lsgwr.exam.vo.UserUpdateVo;
import lsgwr.exam.vo.UserVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@Api(tags = "User APIs")
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @PostMapping("/register")
    @ApiOperation("注册")
    ResultVO<UserVo> register(@RequestBody @Valid RegisterDTO registerDTO) {
        // P0-8 修复 [A-04]：返回 UserVo 而非 User 实体，避免泄露密码哈希
        ResultVO<UserVo> resultVO;
        // 注册信息的完善，还有唯一性校验没(用户名、邮箱和手机号)已经在user表中通过unique来设置了
        User user = userService.register(registerDTO);
        if (user != null) {
            // 注册成功，转换为 UserVo（不含密码字段）
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            resultVO = new ResultVO<>(ResultEnum.REGISTER_SUCCESS.getCode(), ResultEnum.REGISTER_SUCCESS.getMessage(), userVo);
        } else {
            resultVO = new ResultVO<>(ResultEnum.REGISTER_FAILED.getCode(), ResultEnum.REGISTER_FAILED.getMessage(), null);
        }
        return resultVO;
    }

    @PostMapping("/login")
    @ApiOperation("根据用户名或邮箱登录,登录成功返回token")
    ResultVO<String> login(@RequestBody @Valid LoginQo loginQo) { // 这里不用手机号是因为手机号和用户名难以进行格式区分，而用户名和
        // A-10 修复：暴力破解防护——检查是否被锁定
        String attemptKey = loginQo.getUserInfo();
        if (loginAttemptService.isLocked(attemptKey)) {
            throw new ExamException(ResultEnum.LOGIN_FAILED.getCode(),
                    "登录尝试次数过多，账号已被锁定 15 分钟，请稍后再试");
        }
        // 用户登录
        ResultVO<String> resultVO;
        String token = userService.login(loginQo);
        if (token != null) {
            // 登录成功，重置计数
            loginAttemptService.recordSuccess(attemptKey);
            resultVO = new ResultVO<>(ResultEnum.LOGIN_SUCCESS.getCode(), ResultEnum.LOGIN_SUCCESS.getMessage(), token);
        } else {
            // 登录失败，记录失败尝试
            loginAttemptService.recordFailure(attemptKey);
            int remaining = loginAttemptService.getRemainingAttempts(attemptKey);
            String msg = remaining > 0
                    ? ResultEnum.LOGIN_FAILED.getMessage() + "，剩余尝试次数：" + remaining
                    : "登录失败次数过多，账号已被锁定 15 分钟";
            resultVO = new ResultVO<>(ResultEnum.LOGIN_FAILED.getCode(), msg, null);
        }
        return resultVO;
    }

    @GetMapping("/user-info")
    @ApiOperation("获取用户信息")
    ResultVO<UserVo> getUserInfo(HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        UserVo userVo = userService.getUserInfo(userId);
        return new ResultVO<>(ResultEnum.GET_INFO_SUCCESS.getCode(), ResultEnum.GET_INFO_SUCCESS.getMessage(), userVo);
    }

    @GetMapping("/info")
    @ApiOperation("获取用户的详细信息，包括个人信息页面和操作权限")
    ResultVO<UserInfoVo> getInfo(HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        UserInfoVo userInfoVo = userService.getInfo(userId);
        return new ResultVO<>(ResultEnum.GET_INFO_SUCCESS.getCode(), ResultEnum.GET_INFO_SUCCESS.getMessage(), userInfoVo);
    }

    @PostMapping("/change-password")
    @ApiOperation("修改密码：需要校验旧密码")
    ResultVO<Void> changePassword(@RequestBody @Valid ChangePasswordQo qo, HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        boolean ok = userService.changePassword(userId, qo.getOldPassword(), qo.getNewPassword());
        if (ok) {
            // B-11 修复：提示用户重新登录，旧 token 会在 JWT 过期时间（默认 2h）后自动失效
            return new ResultVO<>(ResultEnum.GET_INFO_SUCCESS.getCode(), "密码修改成功，请重新登录", null);
        }
        return new ResultVO<>(ResultEnum.LOGIN_FAILED.getCode(), "旧密码错误", null);
    }

    /**
     * I-10 修复：管理员修改用户角色
     */
    @PostMapping("/role")
    @ApiOperation("管理员修改用户角色（仅管理员）")
    @RoleRequired({RoleEnum.ADMIN})
    ResultVO<Void> updateUserRole(@RequestParam String targetUserId, @RequestParam Integer newRoleId,
                                  HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("user_id");
        userService.updateUserRole(adminUserId, targetUserId, newRoleId);
        return new ResultVO<>(0, "用户角色修改成功", null);
    }

    /**
     * I-11 修复：用户更新个人信息
     */
    @PostMapping("/update")
    @ApiOperation("更新个人信息（昵称、头像、描述、邮箱、手机号）")
    ResultVO<UserVo> updateUserInfo(@RequestBody UserUpdateVo updateVo, HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        UserVo userVo = userService.updateUserInfo(userId, updateVo);
        return new ResultVO<>(0, "个人信息更新成功", userVo);
    }
}
