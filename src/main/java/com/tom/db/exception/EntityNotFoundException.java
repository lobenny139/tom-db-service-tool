package com.tom.db.exception;

public class EntityNotFoundException extends RuntimeException  {

    public EntityNotFoundException(String msg){
        super(msg);
    }

    public EntityNotFoundException(){
        super("Entity does not exist");
    }

    public EntityNotFoundException(String tableName, String columnName, String columnValue ){
        super("無法以" + columnName + "[" + columnValue + "], 在Table[" + tableName + "]中找到任何資料!!!");
    }
}
