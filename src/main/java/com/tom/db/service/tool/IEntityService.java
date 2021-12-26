package com.tom.db.service.tool;

import java.util.List;
import java.util.Map;

public interface IEntityService<T, ID> {

    Iterable<T> getEntitiesBySQL(String sql);

    Iterable<T> getAllEntities();

    T getEntityById(ID id);

    T createEntity(T t);

    T updateEntity(ID id, T t);

    boolean deleteEntity(ID id);

    boolean entityExistsById(ID id);

    List<Map<String, Object>> getBySQLNoWrap(String sql);

}

