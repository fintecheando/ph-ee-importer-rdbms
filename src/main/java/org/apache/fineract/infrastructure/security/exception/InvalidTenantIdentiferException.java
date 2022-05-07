package org.apache.fineract.infrastructure.security.exception;

import org.springframework.dao.EmptyResultDataAccessException;

/**
 * {@link RuntimeException} thrown when an invalid tenant identifier is used in request to platform.
 *
 *
 */
public class InvalidTenantIdentiferException extends RuntimeException {

    public InvalidTenantIdentiferException(final String message) {
        super(message);
    }

    public InvalidTenantIdentiferException(String message, EmptyResultDataAccessException e) {
        super(message, e);
    }
}
