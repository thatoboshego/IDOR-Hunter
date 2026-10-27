package com.idorhunter.service;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(Long id) {
        super("No document with id " + id);
    }
}
