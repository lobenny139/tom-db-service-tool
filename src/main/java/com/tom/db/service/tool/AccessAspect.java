package com.tom.db.service.tool;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Aspect
@Component
@Order(5)
public class AccessAspect {

    private static final Logger logger = LoggerFactory.getLogger(AccessAspect.class);

    /**
     * 該package 下所有class,method都被攔截
     */
    @Pointcut("execution(* com.tom.db.service.tool..*(..))")
    public void pointcut() { }

    @Around("pointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        Object object = null;

        String clazzName = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        if( methodName.toLowerCase().indexOf("get") >= 0 || "getAllEntitys".equals(methodName) || "getEntityById".equals(methodName) || "getEntitysBySQL".equals(methodName) || methodName.indexOf("exists") >= 0){
            logger.info("[{}] - [{}] 開始 SQL 查詢, 查詢參數" + Arrays.toString(joinPoint.getArgs()) , clazzName, methodName);
            object = joinPoint.proceed();
            //logger.info(object.getClass().getCanonicalName());
            int cnt = 0;
            if (object instanceof Collection) {
                Collection col = (Collection) object;
                cnt = col.size();
            } else if(object != null){
                cnt = 1;
            } else if (object instanceof Map) {
                Map map = (Map) object;
                cnt = map.size();
                // do something with list
            }
            logger.info("[{}] - [{}] 結束 SQL 查詢, 成功取得{}筆記錄.", clazzName, methodName, cnt);
//        }else if( "saveEntity".equals(methodName) || "updateEntity".equals(methodName) || "createEntity".equals(methodName) || "createEntities".equals(methodName) || methodName.indexOf("create") >= 0 ) {
        }else if( methodName.toLowerCase().indexOf("save") >= 0 || methodName.toLowerCase().indexOf("update") >= 0 || methodName.toLowerCase().indexOf("create") >= 0) {
            String op = "新增";
//            if (("updateEntity").equals(methodName) ) {
            if(methodName.toLowerCase().indexOf("update") >= 0){
                op = "更新";
            }
            logger.info("[{}] - [{}] 開始" + op + "資料, 資料參數" + Arrays.toString(joinPoint.getArgs()), clazzName, methodName);
            object = joinPoint.proceed();
            if(object != null && object.getClass().getCanonicalName().indexOf("java.util.ArrayList") >=0){
                logger.info("[{}] - [{}] 結束" + op + "資料, 成功" + op + "{}.", clazzName, methodName, ((List)object).size() +"筆記錄" );
            }else{
                logger.info("[{}] - [{}] 結束" + op + "資料, 成功" + op + "[{}].", clazzName, methodName, object.getClass().getCanonicalName());
            }
//        }else if("deleteEntity".equals(methodName)){
        }else if(methodName.toLowerCase().indexOf("delete") >= 0){
            logger.info("[{}] - [{}] 開始刪除資料, 資料參數" + Arrays.toString(joinPoint.getArgs()) , clazzName, methodName);
            object = joinPoint.proceed();
            logger.info("[{}] - [{}] 結束刪除資料, 成功刪除.", clazzName, methodName);
        }else{
            object = joinPoint.proceed();
            logger.warn( clazzName +  "." + methodName + "<--- 這方法沒被攔截到, 請確認." );
        }
        logger.info("[" + clazzName + "] - [" + methodName + "] 執行 SQL 耗時:" + (System.currentTimeMillis() - startTime) + "豪秒");
        return object;
    }


    /**
     * AfterThrowing: 異常通知
     */
    @AfterThrowing(value="pointcut()",throwing="e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e){
        String clazzName = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        logger.error("[" + clazzName + "] - [" + methodName + "] 執行失敗, 執行" + joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName() + " <-- 執行過程中拋出 excepton: " + e.getMessage());
    }

}
