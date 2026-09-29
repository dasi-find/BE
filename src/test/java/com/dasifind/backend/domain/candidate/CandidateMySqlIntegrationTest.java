package com.dasifind.backend.domain.candidate;

import com.dasifind.backend.domain.candidate.entity.Candidate;
import com.dasifind.backend.domain.policeitem.entity.PoliceItem;
import com.dasifind.backend.domain.policeitem.model.PoliceItemSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

/** Opt-in, disposable database only; runs inherited storage/deletion tests on Flyway schema too. */
@EnabledIfEnvironmentVariable(named = "DASI_MYSQL_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${DASI_MYSQL_TEST_URL}",
        "spring.datasource.username=candidate_test",
        "spring.datasource.password=candidate_test_only",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class CandidateMySqlIntegrationTest extends CandidateStorageIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void 전체_Flyway_마이그레이션과_JPA_검증을_통과한다() {
        assertThat(jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class)).isEqualTo(9);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image_score", "text_score", "image_text_score", "attribute_score",
            "date_score", "location_score", "evidence_coverage"})
    void DB에서도_항목별_점수_범위를_강제한다(String column) {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        Candidate saved = candidates.saveAndFlush(candidate(card, item));
        assertThatThrownBy(() -> jdbc.update("update candidate set " + column + " = 1.1 where id = ?", saved.getId()))
                .rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
        assertThatThrownBy(() -> jdbc.update("update candidate set " + column + " = -0.1 where id = ?", saved.getId()))
                .rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
    }

    @Test
    void DB에서_최종점수_범위와_근거없는_최종점수를_거부한다() {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        Candidate saved = candidates.saveAndFlush(candidate(card, item));
        assertThatThrownBy(() -> jdbc.update("update candidate set total_score = 101 where id = ?", saved.getId()))
                .rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
        assertThatThrownBy(() -> jdbc.update("update candidate set evidence_coverage = 0 where id = ?", saved.getId()))
                .rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
        assertThatThrownBy(() -> jdbc.update("""
                update candidate set image_score=null, text_score=null, image_text_score=null,
                    attribute_score=null, date_score=null, location_score=null where id=?
                """, saved.getId())).rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
    }

    @Test
    void 관리번호_대소문자를_보존하고_출처와_순번을_검증한다() {
        PoliceItem first = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        items.saveAndFlush(item(PoliceItemSource.POLICE, "f-1", 1));
        assertThat(items.count()).isEqualTo(2);
        assertThatThrownBy(() -> jdbc.update("update police_item set source='UNKNOWN' where id=?", first.getId()))
                .rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
        assertThatThrownBy(() -> jdbc.update("update police_item set item_sequence=0 where id=?", first.getId()))
                .rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
    }

    @Test
    void 근거_유형과_요소는_DB에서도_검증한다() {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        Candidate saved = candidates.saveAndFlush(candidate(card, item));
        assertThatThrownBy(() -> jdbc.update(
                "update candidate_evidence set evidence_type='UNKNOWN' where candidate_id=?", saved.getId()))
                .rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
        assertThatThrownBy(() -> jdbc.update(
                "update candidate_evidence set score_element='UNKNOWN' where candidate_id=?", saved.getId()))
                .rootCause().isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
    }

    @Test
    void 피드백과_입력버전의_DB_제약을_검증한다() {
        PoliceItem item = items.saveAndFlush(item(PoliceItemSource.POLICE, "F-1", 1));
        Candidate saved = candidates.saveAndFlush(candidate(card, item));
        for (String assignment : java.util.List.of("feedback='UNKNOWN'", "feedback='NOT_MINE'",
                "notification_suppressed=2", "assessed_analysis_id=null", "assessed_police_item_version=-1")) {
            assertThatThrownBy(() -> jdbc.update("update candidate set " + assignment + " where id=?", saved.getId()))
                    .rootCause().isInstanceOfSatisfying(SQLException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(3819));
        }
    }
}
