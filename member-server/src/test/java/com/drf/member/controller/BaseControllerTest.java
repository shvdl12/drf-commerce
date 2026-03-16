package com.drf.member.controller;

import com.drf.common.exception.GlobalExceptionHandler;
import com.drf.common.interceptor.RoleCheckInterceptor;
import com.drf.common.resolver.AuthInfoResolver;
import com.drf.member.common.config.WebConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import({AuthInfoResolver.class, RoleCheckInterceptor.class, WebConfig.class, GlobalExceptionHandler.class})
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
