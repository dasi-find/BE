package com.dasifind.backend.domain.home;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfEnvironmentVariable(named = "DASI_MYSQL_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${DASI_MYSQL_TEST_URL}",
        "spring.datasource.username=candidate_test",
        "spring.datasource.password=candidate_test_only",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "app.search-card.expiration-initial-delay=1h",
        "app.search-card-image.cleanup-initial-delay=1h",
        "app.search-card-image.deletion-cleanup-initial-delay=1h"
})
class HomeApiMySqlIntegrationTest extends HomeApiIntegrationTest {
}
