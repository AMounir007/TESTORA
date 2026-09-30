package com.testora.core.exceptions;

/** Base class for all actionable framework errors. */
public class TestoraException extends RuntimeException {
    public TestoraException(String message) { super(message); }
    public TestoraException(String message, Throwable cause) { super(message, cause); }
}
