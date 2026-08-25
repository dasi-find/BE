package com.dasifind.backend.domain.user.repository;

import com.dasifind.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 이메일의_대소문자를_구분하지_않고_가입_여부를_확인한다() {
        userRepository.saveAndFlush(User.create(
                "user@example.com",
                "encoded-password",
                "민준",
                true
        ));

        assertThat(userRepository.existsByEmailIgnoreCase("USER@EXAMPLE.COM")).isTrue();
    }
}
