package com.tom.db.exception;

public class DataException extends RuntimeException {

    public DataException(){
        super("Data record has some error");
    }

    public DataException(String message){
        super(message);
    }

    public DataException(String tableName, String columnName, String columnValue){
        super("Table[" + tableName + "]中, " + columnName + "[" + columnValue + "]已經存在, 不能新建!!!");
    }
}
