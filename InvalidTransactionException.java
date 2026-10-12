package com.wipro.bankstatement.util;

/**
 * Thrown when a statement line does not match the expected regex pattern.
 */
public class InvalidTransactionException extends Exception {

    private final String rawLine;

    public InvalidTransactionException(String message, String rawLine) {
        super(message);
        this.rawLine = rawLine;
    }

    public String getRawLine() {
        return rawLine;
    }

    @Override
    public String toString() {
        return "InvalidTransactionException: " + getMessage() +
                " | Offending line -> [" + rawLine + "]";
    }
}
