package com.dasifind.backend.domain.candidate;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/** Reuses the HTTP/query suite against a disposable MySQL database and the real migrations. */
@EnabledIfEnvironmentVariable(named = "DASI_MYSQL_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${DASI_MYSQL_TEST_URL}",
        "spring.datasource.username=candidate_test",
        "spring.datasource.password=candidate_test_only",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class CandidateApiMySqlIntegrationTest extends CandidateApiIntegrationTest {
}
