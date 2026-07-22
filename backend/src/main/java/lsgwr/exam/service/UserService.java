/***********************************************************
 * @Description : 用户接口
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-17 08:02
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.service;

import lsgwr.exam.dto.RegisterDTO;
import lsgwr.exam.entity.User;
import lsgwr.exam.qo.LoginQo;
import lsgwr.exam.vo.UserInfoVo;
import lsgwr.exam.vo.UserUpdateVo;
import lsgwr.exam.vo.UserVo;

public interface UserService {
    /**
     * 注册
     *
     * @param registerDTO 注册参数
     * @return 注册成功后的用户信息
     */
    User register(RegisterDTO registerDTO);

    /**
     * 登录接口，登录成功返回token
     *
     * @param loginQo 登录参数
     * @return 成功返回token，失败返回null
     */
    String login(LoginQo loginQo);

    /**
     * 根据用户id获取用户信息
     *
     * @return 用户实体
     */
    UserVo getUserInfo(String userId);

    /**
     * 获取用户详细信息(主要是权限相关的)
     * @param userId 用户的id
     * @return 用户信息组装的实体
     */
    UserInfoVo getInfo(String userId);

    /**
     * 修改密码：校验旧密码后用 BCrypt 重新哈希新密码
     *
     * @param userId      用户id
     * @param oldPassword 旧密码(明文)
     * @param newPassword 新密码(明文)
     * @return true 修改成功；false 旧密码错误或用户不存在
     */
    boolean changePassword(String userId, String oldPassword, String newPassword);

    /**
     * I-10 修复：管理员修改用户角色
     *
     * @param adminUserId  操作管理员id（用于审计日志）
     * @param targetUserId 要修改的用户id
     * @param newRoleId    新角色id（1=ADMIN, 2=TEACHER, 3=STUDENT）
     */
    void updateUserRole(String adminUserId, String targetUserId, Integer newRoleId);

    /**
     * I-11 修复：用户更新个人信息（仅允许修改昵称、头像、描述、邮箱、手机号）
     *
     * @param userId    当前用户id
     * @param updateVo  更新信息VO
     * @return 更新后的用户信息
     */
    UserVo updateUserInfo(String userId, UserUpdateVo updateVo);
}
