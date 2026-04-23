package com.eventpipeline.annotation;

import com.eventpipeline.domain.EventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserEvent {
    EventType type();
    String description() default "";
}
