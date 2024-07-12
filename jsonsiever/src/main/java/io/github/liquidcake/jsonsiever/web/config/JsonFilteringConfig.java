/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package io.github.liquidcake.jsonsiever.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.liquidcake.jsonsiever.core.JsonFilteringService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// @formatter:off
/**
 * Import this config to Spring configuration file <pre>@Import(JsonFilteringConfig.class)</pre>
 * Declare servlet filer and caching bean inside Spring configuration file e.g.
 * <pre>
    {@literal @}Bean
    public FilterRegistrationBean&lt;JsonResponseBodyFilter&gt; filterRegistrationBean(
            JsonResponseFilterApplier jsonResponseFilterApplier
    ) {
        FilterRegistrationBean&lt;JsonResponseBodyFilter&gt; registrationBean = new FilterRegistrationBean&lt;&gt;();

        registrationBean.setFilter(
                new JsonResponseBodyFilter(jsonResponseFilterApplier)
        );

        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(99);
        return registrationBean;
    }

    {@literal @}Bean
    public DynamicJsonFiltersCacheWrapper simpleInMemoryDynamicJsonFiltersCacheWrapper() {
        //provide your own implementation if needed - e.g. wrapper to Redis cache etc
        return new SimpleInMemoryDynamicJsonFiltersCacheWrapper();
    }
 * </pre>
 * */
//@formatter:on
@Configuration
@ComponentScan("io.github.liquidcake.jsonsiever")
public class JsonFilteringConfig {

    @Bean
    public JsonFilteringService jsonFilteringService() {
        //create new ObjectMapper instance to get default JsonFactory from it
        return new JsonFilteringService(new ObjectMapper().getFactory());
    }
}
