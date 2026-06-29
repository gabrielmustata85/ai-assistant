package com.ai.assistant.company;

public class CompanyAccessDeniedException extends RuntimeException {
    public CompanyAccessDeniedException() {
        super("You do not have access to this company's data");
    }
}
