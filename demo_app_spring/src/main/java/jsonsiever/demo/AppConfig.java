package jsonsiever.demo;

import jsonsiever.web.DynamicJsonFiltersCacheWrapper;
import jsonsiever.web.JsonResponseBodyFilter;
import jsonsiever.web.JsonResponseFilterApplier;
import jsonsiever.web.SimpleInMemoryDynamicJsonFiltersCacheWrapper;
import jsonsiever.web.config.JsonFilteringConfig;
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