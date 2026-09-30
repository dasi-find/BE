package com.dasifind.backend.domain.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DASI_MYSQL_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${DASI_MYSQL_TEST_URL}",
        "spring.datasource.username=candidate_test",
        "spring.datasource.password=candidate_test_only",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class NotificationApiMySqlIntegrationTest extends NotificationApiIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void 마이그레이션과_유형_참조제약을_검증한다() {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success=true", Integer.class)).isEqualTo(10);
        var system = system(owner, NOW);
        for (String assignment : java.util.List.of("type='UNKNOWN'", "type='NEW_CANDIDATE'")) {
            assertThatThrownBy(() -> jdbc.update("update notification set " + assignment + " where id=?", system.getId()))
                    .rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
        }
    }
}
