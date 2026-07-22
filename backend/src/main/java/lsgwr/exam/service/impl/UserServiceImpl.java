/***********************************************************
 * @Description : 用户服务
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-17 08:03
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import lsgwr.exam.dto.RegisterDTO;
import lsgwr.exam.entity.Action;
import lsgwr.exam.entity.Page;
import lsgwr.exam.entity.Role;
import lsgwr.exam.entity.User;
import lsgwr.exam.enums.LoginTypeEnum;
import lsgwr.exam.enums.ResultEnum;
import lsgwr.exam.enums.RoleEnum;
import lsgwr.exam.exception.ExamException;
import lsgwr.exam.qo.LoginQo;
import lsgwr.exam.repository.ActionRepository;
import lsgwr.exam.repository.PageRepository;
import lsgwr.exam.repository.RoleRepository;
import lsgwr.exam.repository.UserRepository;
import lsgwr.exam.service.UserService;
import lsgwr.exam.util.AuditLogger;
import lsgwr.exam.utils.JwtUtils;
import lsgwr.exam.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    ActionRepository actionRepository;


    @Override
    public User register(RegisterDTO registerDTO) {
        try {
            User user = new User();
            user.setUserId(IdUtil.simpleUUID());
            // 好像还缺少个用户名,用"exam_user_手机号"来注册：需要校验唯一性数据字段已经设置unique了，失败会异常地
            String defaultUsername = "user";
            user.setUserUsername(defaultUsername + "_" + registerDTO.getMobile());
            // 初始化昵称和用户名相同
            user.setUserNickname(user.getUserUsername());
            // 密码使用 BCrypt 加盐哈希存储（不可逆），告别 Base64 这种可逆编码
            user.setUserPassword(BCrypt.hashpw(registerDTO.getPassword()));
            // 默认设置为学生身份，需要老师和学生身份地话需要管理员修改
            user.setUserRoleId(RoleEnum.STUDENT.getId());
            // 设置头像图片地址, 先默认一个地址，后面用户可以自己再改
            String defaultAvatar = "http://d.lanrentuku.com/down/png/1904/business_avatar/8_avatar_2754583.png";
            user.setUserAvatar(defaultAvatar);
            // 设置描述信息，随便设置段默认的
            user.setUserDescription("welcome to online exam system");
            // 需要验证这个邮箱是不是已经存在：数据字段已经设置unique了，失败会异常地
            user.setUserEmail(registerDTO.getEmail());
            // 需要验证手机号是否已经存在：数据字段已经设置unique了，失败会异常地
            user.setUserPhone(registerDTO.getMobile());
            userRepository.save(user);
            // P-08 修复：审计日志 - 用户注册
            AuditLogger.success(user.getUserId(), "USER_REGISTER", user.getUserId(),
                    "username=" + user.getUserUsername());
            return user;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 唯一约束冲突（用户名/邮箱/手机号已存在）
            log.warn("注册失败-唯一约束冲突: {}", e.getMessage());
            AuditLogger.failure("anonymous", "USER_REGISTER", null, "唯一约束冲突");
            return null;
        } catch (Exception e) {
            // B-10 修复：使用日志替代 printStackTrace，避免泄露堆栈到标准错误流
            log.error("注册失败-系统异常: {}", e.getMessage(), e);
            AuditLogger.failure("anonymous", "USER_REGISTER", null, "系统异常");
            return null;
        }
    }

    @Override
    public String login(LoginQo loginQo) {
        User user;
        if (LoginTypeEnum.USERNAME.getType().equals(loginQo.getLoginType())) {
            // 登陆者用地是用户名
            user = userRepository.findByUserUsername(loginQo.getUserInfo());
        } else {
            // 登陆者用地是邮箱
            user = userRepository.findByUserEmail(loginQo.getUserInfo());
        }
        if (user != null) {
            // 如果user不是null即能找到，才能验证用户名和密码
            String passwordHashed = user.getUserPassword();
            String passwordQo = loginQo.getPassword();
            // 优先使用 BCrypt 校验新密码
            if (BCrypt.checkpw(passwordQo, passwordHashed)) {
                return JwtUtils.genJsonWebToken(user);
            }
            // 兼容历史 Base64 编码密码：校验通过后自动迁移为 BCrypt 哈希
            try {
                String passwordDbLegacy = cn.hutool.core.codec.Base64.decodeStr(passwordHashed);
                if (passwordQo.equals(passwordDbLegacy)) {
                    // 自动迁移为 BCrypt
                    user.setUserPassword(BCrypt.hashpw(passwordQo));
                    userRepository.save(user);
                    return JwtUtils.genJsonWebToken(user);
                }
            } catch (Exception ignored) {
                // 非 Base64 哈希，忽略异常
            }
        }
        return null;
    }

    @Override
    public UserVo getUserInfo(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        // B-10 修复：用显式异常替代 assert（assert 在生产环境默认不启用）
        if (user == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "用户不存在");
        }
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }

    @Override
    public UserInfoVo getInfo(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        // B-10 修复：用显式异常替代 assert
        if (user == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "用户不存在");
        }
        UserInfoVo userInfoVo = new UserInfoVo();
        // 1.尽可能的拷贝属性
        BeanUtils.copyProperties(user, userInfoVo);
        Integer roleId = user.getUserRoleId();
        Role role = roleRepository.findById(roleId).orElse(null);
        // B-10 修复：用显式异常替代 assert
        if (role == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "用户角色不存在");
        }
        String roleName = role.getRoleName();

        // 2.设置角色名称
        userInfoVo.setRoleName(roleName);

        // 3.设置当前用户的角色细节
        RoleVo roleVo = new RoleVo();
        BeanUtils.copyProperties(role, roleVo);

        // 4.设置角色的可访问页面
        String rolePageIds = role.getRolePageIds();
        String[] pageIdArr = rolePageIds.split("-");
        List<PageVo> pageVoList = new ArrayList<>();
        for (String pageIdStr : pageIdArr) {
            // 获取页面的id
            Integer pageId = Integer.parseInt(pageIdStr);

            // 4.1 向Role中添加Page
            Page page = pageRepository.findById(pageId).orElse(null);
            PageVo pageVo = new PageVo();
            BeanUtils.copyProperties(page, pageVo);

            // 4.2 向Page中添加action
            List<ActionVo> actionVoList = new ArrayList<>();
            String actionIdsStr = page.getActionIds();
            String[] actionIdArr = actionIdsStr.split("-");
            for (String actionIdStr : actionIdArr) {
                Integer actionId = Integer.parseInt(actionIdStr);
                Action action = actionRepository.findById(actionId).orElse(null);
                ActionVo actionVo = new ActionVo();
                // B-10 修复：用显式异常替代 assert
                if (action == null) {
                    throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "操作权限不存在");
                }
                BeanUtils.copyProperties(action, actionVo);
                actionVoList.add(actionVo);
            }
            // 设置actionVoList到pageVo中，然后把pageVo加到pageVoList中
            pageVo.setActionVoList(actionVoList);
            // 设置pageVoList，下面再设置到RoleVo中
            pageVoList.add(pageVo);
        }
        // 设置PageVo的集合到RoleVo中
        roleVo.setPageVoList(pageVoList);
        // 最终把PageVo设置到UserInfoVo中，这样就完成了拼接
        userInfoVo.setRoleVo(roleVo);
        return userInfoVo;
    }

    @Override
    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        String passwordHashed = user.getUserPassword();
        // 优先用 BCrypt 校验
        boolean matched = BCrypt.checkpw(oldPassword, passwordHashed);
        // 兼容历史 Base64 编码密码
        if (!matched) {
            try {
                String legacy = cn.hutool.core.codec.Base64.decodeStr(passwordHashed);
                matched = oldPassword.equals(legacy);
            } catch (Exception ignored) {
                // 非 Base64 哈希
            }
        }
        if (!matched) {
            // P-08 修复：审计日志 - 修改密码失败（旧密码错误）
            AuditLogger.failure(userId, "PASSWORD_CHANGE", userId, "旧密码错误");
            return false;
        }
        // 使用 BCrypt 重新哈希新密码
        user.setUserPassword(BCrypt.hashpw(newPassword));
        userRepository.save(user);
        // P-08 修复：审计日志 - 修改密码成功
        AuditLogger.success(userId, "PASSWORD_CHANGE", userId, "成功");
        return true;
    }

    /**
     * I-10 修复：管理员修改用户角色
     */
    @Override
    public void updateUserRole(String adminUserId, String targetUserId, Integer newRoleId) {
        User user = userRepository.findById(targetUserId).orElse(null);
        if (user == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "目标用户不存在");
        }
        // 校验角色ID合法性（1=ADMIN, 2=TEACHER, 3=STUDENT）
        if (newRoleId == null
                || (!newRoleId.equals(RoleEnum.ADMIN.getId())
                && !newRoleId.equals(RoleEnum.TEACHER.getId())
                && !newRoleId.equals(RoleEnum.STUDENT.getId()))) {
            throw new ExamException(ResultEnum.PARAM_ERR.getCode(), "非法的角色ID");
        }
        Integer oldRoleId = user.getUserRoleId();
        user.setUserRoleId(newRoleId);
        userRepository.save(user);
        log.info("管理员{}修改用户角色：用户{}的角色改为{}", adminUserId, targetUserId, newRoleId);
        // P-08 修复：审计日志 - 管理员修改用户角色
        AuditLogger.success(adminUserId, "ROLE_CHANGE", targetUserId,
                "from=" + oldRoleId + ",to=" + newRoleId);
    }

    /**
     * I-11 修复：用户更新个人信息（仅允许修改昵称、头像、描述、邮箱、手机号）
     */
    @Override
    public UserVo updateUserInfo(String userId, UserUpdateVo updateVo) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "用户不存在");
        }
        // 仅更新非 null 字段，避免覆盖未传入的字段
        if (updateVo.getUserNickname() != null) {
            user.setUserNickname(updateVo.getUserNickname());
        }
        if (updateVo.getUserAvatar() != null) {
            user.setUserAvatar(updateVo.getUserAvatar());
        }
        if (updateVo.getUserDescription() != null) {
            user.setUserDescription(updateVo.getUserDescription());
        }
        if (updateVo.getUserEmail() != null) {
            user.setUserEmail(updateVo.getUserEmail());
        }
        if (updateVo.getUserPhone() != null) {
            user.setUserPhone(updateVo.getUserPhone());
        }
        userRepository.save(user);
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }
}
