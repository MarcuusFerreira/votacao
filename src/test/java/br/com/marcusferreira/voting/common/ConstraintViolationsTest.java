package br.com.marcusferreira.voting.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

class ConstraintViolationsTest {

    @Test
    void readsTheConstraintReportedByPostgres() {
        PSQLException driverError = new PSQLException(
            new ServerErrorMessage("SERROR\0C23505\0Mduplicate key value\0nuk_votes_session_cpf\0"));

        assertThat(ConstraintViolations.violates(new DuplicateKeyException("duplicate", driverError), "uk_votes_session_cpf")).isTrue();
        assertThat(ConstraintViolations.violates(new DuplicateKeyException("duplicate", driverError), "uk_votes_session_member")).isFalse();
    }

    @Test
    void readsTheConstraintReportedByHibernate() {
        DataIntegrityViolationException error = new DataIntegrityViolationException("violation",
            new ConstraintViolationException("violation", new SQLException(), "uk_voting_sessions_agenda"));

        assertThat(ConstraintViolations.violates(error, "uk_voting_sessions_agenda")).isTrue();
    }

    @Test
    void ignoresTheMessageText() {
        DuplicateKeyException error = new DuplicateKeyException("mentions uk_votes_session_cpf only in the text");

        assertThat(ConstraintViolations.violates(error, "uk_votes_session_cpf")).isFalse();
    }
}
