package io.github.liquidcake.jsonsiever.demo;

import io.github.liquidcake.jsonsiever.web.DynamicJsonFiltersCacheWrapper;
import io.github.liquidcake.jsonsiever.web.JsonResponseBodyFilter;
import io.github.liquidcake.jsonsiever.web.JsonResponseFilterApplier;
import io.github.liquidcake.jsonsiever.web.SimpleInMemoryDynamicJsonFiltersCacheWrapper;
import io.github.liquidcake.jsonsiever.web.config.JsonFilteringConfig;
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