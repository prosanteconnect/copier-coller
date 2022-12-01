package fr.ans.psc.context.sharing.api.exception;

import org.springframework.http.HttpStatus;

public class PscMissingAccessTokenException extends PscContextSharingException {
    public PscMissingAccessTokenException() {
        super();
    }

    public PscMissingAccessTokenException(HttpStatus status) {
        super(status);
    }
}
