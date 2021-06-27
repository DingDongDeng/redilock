package com.dingdongdeng.redilock.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Sync {

    /**
     * redisson lock 키 생성을 위한 값
     * key를 찾기 위한 필드명을 설정
     * (ex: model.param.key)
     * @return
     */
    String key() default "id";
    long limit() default 100;
}
