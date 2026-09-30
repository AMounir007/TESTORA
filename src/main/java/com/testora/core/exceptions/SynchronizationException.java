package com.testora.core.exceptions;

/** Thrown when a wait times out. The message is the full wait diagnostics report. */
public class SynchronizationException extends TestoraException {
    public SynchronizationException(String message, Throwable cause) { super(message, cause); }
}
