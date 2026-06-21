package org.hagelbrand.service;

public class DuplicateEmailException extends RuntimeException {

    private final String existingProvider;

    public DuplicateEmailException(String existingProvider) {
        super("Email already linked to an account via " + existingProvider);
        this.existingProvider = existingProvider;
    }

    public String getExistingProvider() {
        return existingProvider;
    }
}
