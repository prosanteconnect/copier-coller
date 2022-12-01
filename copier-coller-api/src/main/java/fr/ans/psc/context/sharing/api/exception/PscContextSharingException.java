package fr.ans.psc.context.sharing.api.exception;

import org.springframework.http.HttpStatus;

public class PscContextSharingException extends Exception {
    private HttpStatus status;

    public PscContextSharingException() {
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public PscContextSharingException(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }
}
