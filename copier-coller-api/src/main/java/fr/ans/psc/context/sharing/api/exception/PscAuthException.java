package fr.ans.psc.context.sharing.api.exception;

import org.springframework.http.HttpStatus;

public class PscAuthException extends PscContextSharingException {

    public PscAuthException() {
        super();
    }

    public PscAuthException(HttpStatus status) {
        super(status);
    }
}
