package com.shop.unit;

import com.shop.dto.UserLoginDTO;
import com.shop.dto.UserRegisterDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("用户字段校验单元测试")
class UserValidationUnitTest extends UnitTestValidationSupport {

    @Test
    @DisplayName("UT-USER-001 合法用户名、密码和确认密码应通过注册字段校验")
    void validRegisterDtoPassesValidation() {
        UserRegisterDTO dto = registerDto("user_01", "Aa123456@", "Aa123456@");

        assertTrue(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-002 用户名长度为5位应校验失败")
    void usernameWithFiveCharactersFailsValidation() {
        UserRegisterDTO dto = registerDto("user1", "Aa123456@", "Aa123456@");

        assertFalse(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-003 用户名长度为6位应校验通过")
    void usernameWithSixCharactersPassesValidation() {
        UserRegisterDTO dto = registerDto("user01", "Aa123456@", "Aa123456@");

        assertTrue(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-004 用户名长度为20位应校验通过")
    void usernameWithTwentyCharactersPassesValidation() {
        UserRegisterDTO dto = registerDto("user_01234567890123", "Aa123456@", "Aa123456@");

        assertTrue(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-005 用户名长度为21位应校验失败")
    void usernameWithTwentyOneCharactersFailsValidation() {
        UserRegisterDTO dto = registerDto("user_0123456789012345", "Aa123456@", "Aa123456@");

        assertFalse(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-006 用户名以数字开头应校验失败")
    void usernameStartingWithNumberFailsValidation() {
        UserRegisterDTO dto = registerDto("1userabc", "Aa123456@", "Aa123456@");

        assertFalse(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-007 用户名包含连字符应校验失败")
    void usernameContainingHyphenFailsValidation() {
        UserRegisterDTO dto = registerDto("user-name", "Aa123456@", "Aa123456@");

        assertFalse(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-008 合法复杂密码应校验通过")
    void validPasswordPassesValidation() {
        UserRegisterDTO dto = registerDto("user_01", "Aa123456@", "Aa123456@");

        assertTrue(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-009 密码长度为7位应校验失败")
    void passwordWithSevenCharactersFailsValidation() {
        UserRegisterDTO dto = registerDto("user_01", "Aa1234@", "Aa1234@");

        assertFalse(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-010 密码长度为8位且符合规则应校验通过")
    void passwordWithEightCharactersPassesValidation() {
        UserRegisterDTO dto = registerDto("user_01", "Aa12345@", "Aa12345@");

        assertTrue(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-011 密码缺少大写字母应校验失败")
    void passwordWithoutUppercaseFailsValidation() {
        UserRegisterDTO dto = registerDto("user_01", "aa123456@", "aa123456@");

        assertFalse(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-012 密码缺少小写字母应校验失败")
    void passwordWithoutLowercaseFailsValidation() {
        UserRegisterDTO dto = registerDto("user_01", "AA123456@", "AA123456@");

        assertFalse(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-013 密码缺少数字应校验失败")
    void passwordWithoutDigitFailsValidation() {
        UserRegisterDTO dto = registerDto("user_01", "Aabcdefg@", "Aabcdefg@");

        assertFalse(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-USER-014 密码缺少特殊字符应校验失败")
    void passwordWithoutSpecialCharacterFailsValidation() {
        UserRegisterDTO dto = registerDto("user_01", "Aa1234567", "Aa1234567");

        assertFalse(validate(dto).isEmpty());
    }

    @Test
    @DisplayName("UT-LOGIN-008 登录用户名或密码为空应校验失败")
    void blankLoginFieldsFailValidation() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("");
        dto.setPassword("");

        assertFalse(validate(dto).isEmpty());
    }

    private static UserRegisterDTO registerDto(String username, String password, String confirmPassword) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setConfirmPassword(confirmPassword);
        return dto;
    }
}
