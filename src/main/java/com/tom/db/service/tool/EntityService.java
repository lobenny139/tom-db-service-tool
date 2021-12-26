package com.tom.db.service.tool;


import com.tom.db.exception.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;

@Getter
@Setter
@Component
@Service
public class EntityService<T,ID> {

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired(required = true)
    protected JpaRepository repository;

    protected T saveEntity(T entity) {
        if(getRepository() == null){
            throw new RuntimeException("找不到 JpaRepository");
        }
        try {
            return (T) getRepository().save(entity);
        }catch(Exception e){
            Throwable throwable = findExceptionRoorCause(e);
            if(throwable != null &&
                    (
                            throwable.getMessage().indexOf("Data truncation")>= 0 ||
                                    throwable.getMessage().indexOf("Can't add or update")>= 0)
            ){
                throw new DataException(throwable.getMessage());
            }else if(throwable != null && throwable instanceof SQLIntegrityConstraintViolationException){
                throw new DuplicatedException(throwable.getMessage() );
            }else{
                throw e;
            }
        }
    }

    /**
     * 取出所有物件
     * @return
     */
    public Iterable<T> getAllEntities() {
        return getRepository().findAll();
    }

    /**
     * 增
     * @param t
     * @return
     */
    public T createEntity(T t) {
        //idValue == null means it is auto generate
        Object idValue = getIdValue(t);
        if(idValue != null && entityExistsById((ID) idValue )){
            throw new DataException( getChildsGenericClass() .getSimpleName(), "key",  idValue.toString() );
        }
        return saveEntity(t);
    }

    /**
     * 修
     * @param id
     * @param t
     * @return
     */
    public T updateEntity(ID id, T t) {
        if(! entityExistsById(id)){
            throw new EntityNotFoundException(getChildsGenericClass().getSimpleName(), "id", id.toString());
        }else{
            return saveEntity(t);
        }
    }

    /**
     * 查 by id
     * @param id
     * @return
     */
    public T getEntityById(ID id)  {
        Optional<T> o = getRepository().findById(id);
        if( o.isPresent() ){
            return (T)o.get();
        }else{
            throw new EntityNotFoundException(getChildsGenericClass().getSimpleName(), "id", id.toString());
        }
    }

    /**
     * 以id檢查這個物件在不在
     * @param id
     * @return
     */
    public boolean entityExistsById(ID id) {
        return getRepository().existsById(id);
    }

    /**
     * 刪 by id
     * @param id
     * @return
     */
    public boolean deleteEntity(ID id) {
        try {
            getRepository().deleteById(id);
            return true;
        }catch(EmptyResultDataAccessException emptyResultDataAccessException){
            throw new EntityNotFoundException(getChildsGenericClass().getSimpleName(), "id", id.toString());
        }catch(Exception e){
            throw e;
        }
    }


    /*
     *  取得實作的類別
     *  reference: https://www.itdaan.com/tw/9bdf9dcd79d860bce05c708ddb2e4bda
     */
    protected Class<Object> getChildsGenericClass() {
        return (Class)((ParameterizedType)
                (this.getClass().getGenericSuperclass()))
                .getActualTypeArguments()[0];
    }

    /**
     * 以 SQL 取得物件 List
     * @param sql
     * @return
     */
    public Iterable<T> getEntitiesBySQL(String sql) {
        try{
            return  getEntityManager().createNativeQuery(sql, getChildsGenericClass()).getResultList();
        }catch(PersistenceException e){
            throw new SQLGrammerConflictException((sql + "<-- has somthing wrong, " + e.getMessage()));
        }
    }

    protected String formatInputDateToDBDate(String yyyymmddhhmmss){
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = inputFormat.parse(yyyymmddhhmmss);
            return outputFormat.format(date);
        } catch (ParseException e) {
            throw new UnSupportException("Unsupport date format[" + yyyymmddhhmmss +"].");
        }
    }

    protected Date formatInputDateToDateObject(String yyyymmddhhmmss){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = null;
        try {
            return simpleDateFormat.parse(yyyymmddhhmmss);
        } catch (ParseException e) {
            throw new UnSupportException("Unsupport date format[" + yyyymmddhhmmss +"].");
        }
    }


    /**
     * 以 SQL 取得 data, 不封裝
     * @param sql
     * @return
     */
    @Transactional
    public List<Map<String, Object>> getBySQLNoWrap(String sql) {
        List<String> columnNames = new LinkedList<String>();
        List<Map<String, Object>> results = new LinkedList<>();

        // Hibernate session object to start the db transaction.
        Session hibernateSession = getEntityManager().unwrap(Session.class);

        // Hibernate's doWork() method performs the CRUD operations in the database!
        hibernateSession.doWork(new Work(){
            @Override
            public void execute(Connection connection)  {
                Map<String, Object> result = null;
                Statement statement = null;
                ResultSet resultSet = null;
                ResultSetMetaData rsmd = null;
                try {
                    statement = connection.createStatement();
                    resultSet = statement.executeQuery(sql);
                    rsmd = resultSet.getMetaData();
                    int columnCount = rsmd.getColumnCount();
                    // The column count starts from 1
                    for (int i = 1; i <= columnCount; i++ ) {
                        columnNames.add(rsmd.getColumnLabel(i));
                    }
                    while (resultSet.next()) {
                        result = new HashMap<String, Object>();
                        for(String columnName:columnNames){
                            result.put(columnName, resultSet.getObject(columnName));
                        }
                        results.add(result);
                    }
                } catch (SQLException e) {
                    throw new SQLGrammerConflictException((sql + "<-- somthing wrong, " + e.getMessage()));
                }
            }
        });
        return results;
    }

    /*
     * 取得 exception root cause
     * Ref: https://www.baeldung.com/java-exception-root-cause
     * Find an Exception’s Root Cause
     */
    protected Throwable findExceptionRoorCause(Throwable throwable) {
        Objects.requireNonNull(throwable);
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /**
     * 取得 Entity ID 價
     * @param entity
     * @return
     */
    protected Object getIdValue(Object entity)  {
        Class clazz = entity.getClass();
        Object value = null;
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getAnnotation(javax.persistence.Id.class) != null) {
                field.setAccessible(true);
                try {
                    value = field.get(entity);
                    break;
                } catch (Exception e) {

                }
            }
        }
        return value;
    }
}