package com.github.liquidcake.jsonsiever.demo;

import com.github.liquidcake.jsonsiever.web.DynamicJsonFiltersCacheWrapper;
import com.github.liquidcake.jsonsiever.web.JsonResponseBodyFilter;
import com.github.liquidcake.jsonsiever.web.JsonResponseFilterApplier;
import com.github.liquidcake.jsonsiever.web.SimpleInMemoryDynamicJsonFiltersCacheWrapper;
import com.github.liquidcake.jsonsiever.web.config.JsonFilteringConfig;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@Import(JsonFilteringConfig.class)
@EnableWebMvc
public class AppConfig {

    @Bean
    public FilterRegistrationBean<JsonResponseBodyFilter> filterRegistrationBean(
            JsonResponseFilterApplier jsonResponseFilterApplier
    ) {
        FilterRegistrationBean<JsonResponseBodyFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(
                new JsonResponseBodyFilter(jsonResponseFilterApplier)
        );

        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(99);
        return registrationBean;
    }

    @Bean
    public DynamicJsonFiltersCacheWrapper simpleInMemoryDynamicJsonFiltersCacheWrapper() {
        return new SimpleInMemoryDynamicJsonFiltersCacheWrapper();
    }
}