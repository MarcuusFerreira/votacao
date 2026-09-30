package br.com.marcusferreira.voting.common;

import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

/**
 * Tells which database constraint an integrity error violated, using the constraint name the
 * driver reports (never the message text, whose format is not a contract).
 */
public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    public static boolean violates(Throwable error, String constraint) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String name = constraintName(cause);
            if (name != null) {
                return constraint.equalsIgnoreCase(name);
            }
        }
        return false;
    }

    private static String constraintName(Throwable cause) {
        if (cause instanceof ConstraintViolationException hibernate) {
            return hibernate.getConstraintName();
        }
        if (cause instanceof PSQLException postgres) {
            ServerErrorMessage serverError = postgres.getServerErrorMessage();
            return serverError != null ? serverError.getConstraint() : null;
        }
        return null;
    }
}
