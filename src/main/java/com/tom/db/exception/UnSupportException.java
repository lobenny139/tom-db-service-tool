package com.tom.db.exception;

public class UnSupportException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public UnSupportException() {
        super("Method Not Allowed");
    }

    public UnSupportException(String message) {
        super(message);
    }
}
