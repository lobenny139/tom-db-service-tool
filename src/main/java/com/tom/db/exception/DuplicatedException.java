package com.tom.db.exception;

public class DuplicatedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DuplicatedException(){
        super("Entity duplicate");
    }

    public DuplicatedException(String message){
        super(message);
    }
}
