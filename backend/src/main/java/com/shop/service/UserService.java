package com.shop.service;

import com.shop.dao.UserDao;
import com.shop.dto.UserRegisterDTO;
import com.shop.entity.User;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;

public class UserService {

    private final UserDao userDao;

    // 构造函数注入，方便后续单元测试时传入 Mock 的 UserDao
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * 用户注册逻辑
     */
    public void register(UserRegisterDTO dto) {
        // 1. 检查用户名是否已存在 (业务规则)
        User existingUser = userDao.findByUsername(dto.getUsername());
        if (existingUser != null) {
            throw new IllegalArgumentException("该用户名已被注册");
        }

        // 2. 检查两次密码是否一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }

        // 3. 构建用户实体，并对密码进行 BCrypt 加密
        User newUser = new User();
        newUser.setUsername(dto.getUsername());

        // gensalt() 默认工作因子是 10，兼顾安全与性能
        String hashed = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt());
        newUser.setPasswordHash(hashed);

        // 4. 持久化到数据库
        userDao.save(newUser);
    }

    /**
     * 用户登录逻辑
     * @return 登录成功返回 User 实体，失败则抛出包含具体原因的异常
     */
    public User login(String username, String password) {
        User user = userDao.findByUsername(username);

        // 1. 用户不存在
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 2. 检查账号是否被锁定
        if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            throw new IllegalStateException("账号已锁定，请 15 分钟后再试");
        }

        // 3. 校验密码
        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            // 登录成功：清空失败次数和锁定时间
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            userDao.update(user);
            return user;
        } else {
            // 登录失败：增加失败次数
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);

            // 业务边界值：如果达到 5 次，锁定 15 分钟
            if (attempts >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                userDao.update(user);
                throw new IllegalStateException("账号已锁定，请 15 分钟后再试");
            } else {
                userDao.update(user);
                throw new IllegalArgumentException("用户名或密码错误");
            }
        }
    }
}