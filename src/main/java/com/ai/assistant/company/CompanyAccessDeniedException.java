package com.ai.assistant.company;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CompanyAccessDeniedException extends RuntimeException {
    public CompanyAccessDeniedException() {
        super("You do not have access to this company's data");
    }
}
