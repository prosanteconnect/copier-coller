package fr.ans.psc.context.sharing.api.exception;

import org.springframework.http.HttpStatus;

public class PscCacheException extends PscContextSharingException {
    public PscCacheException() {
        super();
    }

    public PscCacheException(HttpStatus status) {
        super(status);
    }
}
