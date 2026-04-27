package com.app.exam.integration;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockStudentSecurityContextFactory.class)
public @interface WithMockStudent {
    String id() default "00000000-0000-0000-0000-000000000001";
    String[] authorities() default {"STUDENT"};
}
