package com.tom.db.exception;

public class SQLGrammerConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SQLGrammerConflictException(){
        super("SQL Grammer conflict");
    }

    public SQLGrammerConflictException(String message){
        super(message);
    }

}