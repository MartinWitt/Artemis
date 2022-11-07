package de.tum.in.www1.artemis.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ApiVersionRangeNotValidException extends RuntimeException {

    @Override
    public String toString() {
        return super.toString() + "\n The version range must contain one or two items. None or more were found.";
    }
}
