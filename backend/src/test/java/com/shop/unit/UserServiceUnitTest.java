package com.shop.unit;

import com.shop.dao.UserDao;
import com.shop.dto.UserRegisterDTO;
import com.shop.entity.User;
import com.shop.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceUnitTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("UT-USER-015 用户名已存在时注册应失败")
    void registerFailsWhenUsernameAlreadyExists() {
        when(userDao.findByUsername("user_01")).thenReturn(new User());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(registerDto("user_01", "Aa123456@", "Aa123456@"))
        );

        assertEquals("该用户名已被注册", ex.getMessage());
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    @DisplayName("UT-USER-016 两次密码不一致时注册应失败")
    void registerFailsWhenPasswordsDoNotMatch() {
        when(userDao.findByUsername("user_01")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(registerDto("user_01", "Aa123456@", "Aa123456#"))
        );

        assertEquals("两次输入的密码不一致", ex.getMessage());
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    @DisplayName("UT-USER-017 注册成功时应保存加密后的用户密码")
    void registerSavesUserWithEncryptedPassword() {
        when(userDao.findByUsername("user_01")).thenReturn(null);

        userService.register(registerDto("user_01", "Aa123456@", "Aa123456@"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("user_01", saved.getUsername());
        assertNotNull(saved.getPasswordHash());
        assertTrue(BCrypt.checkpw("Aa123456@", saved.getPasswordHash()));
    }

    @Test
    @DisplayName("UT-LOGIN-001 用户名不存在时登录失败")
    void loginFailsWhenUserDoesNotExist() {
        when(userDao.findByUsername("missing")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login("missing", "Aa123456@")
        );

        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("UT-LOGIN-002 密码正确时登录成功并清空失败状态")
    void loginSucceedsWithCorrectPasswordAndClearsFailureState() {
        User user = userWithPassword("user_01", "Aa123456@");
        user.setFailedAttempts(3);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        when(userDao.findByUsername("user_01")).thenReturn(user);

        User result = userService.login("user_01", "Aa123456@");

        assertSame(user, result);
        assertEquals(0, user.getFailedAttempts());
        assertEquals(null, user.getLockedUntil());
        verify(userDao).update(user);
    }

    @Test
    @DisplayName("UT-LOGIN-003 密码错误且未达锁定阈值时失败次数加一")
    void loginFailureIncrementsFailedAttemptsBeforeLockThreshold() {
        User user = userWithPassword("user_01", "Aa123456@");
        user.setFailedAttempts(0);
        when(userDao.findByUsername("user_01")).thenReturn(user);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login("user_01", "Wrong123@")
        );

        assertEquals("用户名或密码错误", ex.getMessage());
        assertEquals(1, user.getFailedAttempts());
        assertEquals(null, user.getLockedUntil());
        verify(userDao).update(user);
    }

    @Test
    @DisplayName("UT-LOGIN-004 第4次失败不锁定账号")
    void fourthLoginFailureDoesNotLockAccount() {
        User user = userWithPassword("user_01", "Aa123456@");
        user.setFailedAttempts(3);
        when(userDao.findByUsername("user_01")).thenReturn(user);

        assertThrows(IllegalArgumentException.class, () -> userService.login("user_01", "Wrong123@"));

        assertEquals(4, user.getFailedAttempts());
        assertEquals(null, user.getLockedUntil());
    }

    @Test
    @DisplayName("UT-LOGIN-005 第5次失败应锁定账号15分钟")
    void fifthLoginFailureLocksAccount() {
        User user = userWithPassword("user_01", "Aa123456@");
        user.setFailedAttempts(4);
        when(userDao.findByUsername("user_01")).thenReturn(user);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> userService.login("user_01", "Wrong123@")
        );

        assertEquals("账号已锁定，请 15 分钟后再试", ex.getMessage());
        assertEquals(5, user.getFailedAttempts());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now().plusMinutes(14)));
        assertTrue(user.getLockedUntil().isBefore(LocalDateTime.now().plusMinutes(16)));
    }

    @Test
    @DisplayName("UT-LOGIN-006 锁定期内即使密码正确也应禁止登录")
    void lockedUserCannotLoginEvenWithCorrectPassword() {
        User user = userWithPassword("user_01", "Aa123456@");
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userDao.findByUsername("user_01")).thenReturn(user);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> userService.login("user_01", "Aa123456@")
        );

        assertEquals("账号已锁定，请 15 分钟后再试", ex.getMessage());
        verify(userDao, never()).update(any(User.class));
    }

    private static UserRegisterDTO registerDto(String username, String password, String confirmPassword) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setConfirmPassword(confirmPassword);
        return dto;
    }

    private static User userWithPassword(String username, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        return user;
    }
}
