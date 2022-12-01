package fr.ans.psc.context.sharing.api.exception;

import org.springframework.http.HttpStatus;

public class PscSchemaException extends PscContextSharingException {
    public PscSchemaException() {
        super();
    }

    public PscSchemaException(HttpStatus status) {
        super(status);
    }
}
