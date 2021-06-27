package com.dingdongdeng.redilock.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Aspect
@RequiredArgsConstructor
public class SyncAspect {

    private final String PREFIX = "REDILOCK_SYNC:";
    private final RedissonClient redilockRedissonClient;

    @Around("@annotation(Sync)")
    public Object syncProcess(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        //fixme 다른프로젝트에서도 사용할수 있도록 모듈화...어떻게할까??

        Object target = getKeyArgument(proceedingJoinPoint); // @Sync가 달린 메소드의 파라미터 중 사용할 key가 존재하는 파라미터를 가져옴
        String key = getKey(target, proceedingJoinPoint); //argument가 Object인 경우 필드의 내용을 확인하여 key를 가져옴
        return redissonLockProcess(proceedingJoinPoint, key); // redisson lock을 걸고 로직 실행
    }

    private Object getKeyArgument(ProceedingJoinPoint proceedingJoinPoint) {

        Object[] args = proceedingJoinPoint.getArgs(); // @Sync가 달린 메소드의 arguments
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Parameter[] parameters = method.getParameters(); //@Sync가 달린 메소드의 Parameters
        String fullFieldName = method.getAnnotation(Sync.class).key(); //key 값이 있는 필드의 이름
        String rootFieldName = StringUtils.split(fullFieldName, ".")[0];//key를 찾기 위한 필드명들 중 가장 상위의 이름

        int index = 0;
        for (Parameter parameter : parameters) {
            if (StringUtils.equals(rootFieldName, parameter.getName())) {
                break;
            }
            index++;
        }
        if (index > args.length) {
            throw new NoSuchElementException("Can't find redilock key");
        }

        return args[index];
    }

    private String getKey(Object target, ProceedingJoinPoint proceedingJoinPoint) throws Exception {

        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String fullFieldName = method.getAnnotation(Sync.class).key(); //key 값이 있는 필드의 이름
        String[] fieldNames = StringUtils.split(fullFieldName, "."); //key를 찾기 위한 필드명들

        if (fieldNames.length == 1) {
            return target.toString();
        }

        Object targetValue = target;
        for (int depth = 1; depth < fieldNames.length; depth++) {
            String fieldName = fieldNames[depth];
            Field field = targetValue.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            targetValue = field.get(targetValue);
        }

        return targetValue.toString();
    }

    private Object redissonLockProcess(ProceedingJoinPoint proceedingJoinPoint, String key) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        long limit = method.getAnnotation(Sync.class).limit(); //redisson lock 최대시간

        RLock lock = redilockRedissonClient.getLock(PREFIX + key);
        log.debug("LOCK ::: lock key : {}, limit : {}ms", PREFIX + key, limit);
        lock.lock(limit, TimeUnit.MILLISECONDS);

        Object result;
        try {
            result = proceedingJoinPoint.proceed();
        } catch (Exception e) {
            throw e;
        } finally {
            log.debug("UNLOCK ::: lock key : {}, limit : {}ms", PREFIX + key, limit);
            lock.unlock();
        }
        return result;
    }
}
