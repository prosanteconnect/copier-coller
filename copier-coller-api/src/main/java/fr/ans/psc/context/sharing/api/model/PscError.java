package fr.ans.psc.context.sharing.api.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter @Setter
public class PscError {
    private HttpStatus statusCode;

    public PscError() {
        this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public PscError(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }
}
