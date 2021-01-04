package com.alibaba.testable.core.annotation;

import javax.lang.model.type.NullType;
import java.lang.annotation.*;

/**
 * Mark method as mock method
 *
 * @author flin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface MockMethod {

    /**
     * mock specified method instead of method with same name
     * @return target method name
     */
    String targetMethod() default "";

    /**
     * explicit set target class instead of adding to parameter list
     * @return target class type
     */
    Class<?> targetClass() default NullType.class;

}
