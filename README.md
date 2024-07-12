# jsonsiever
Dynamic Json filtering library for Java/Spring  
This lib will be useful in a situation when we want to filter (potentially large and complex) json object using kind of WYSIWYG filtering pattern.  

E.g. we have following json and want to remove `agi` field:
```
{
  "name": "orange",
  "stats": {
    "str": 8,
    "agi": 10
  }
}
```
Then filter would be like this:
```
{
  "name": 1,
  "stats": {
    "str": 1
  }
}
```
Removing `name` may look like this:
```
{
  "stats": {}
}
```

## Main use-case:
Lets assume there is a Spring API with some heavy endpoint and we want to remove particular deeply-nested fields from the response, to make it lighter and more easily parsible.
Sometimes we can change our response structure, prevent population of some fields etc. But this is not always an option.
Another possible solution would be to wrap our endpoint in a GraphQL query and let user chose necessary fields for each request. But this approach also has its downsides.  

This library is meant to be "easy to plug in" configurable alternative that allows both static (based on pre-defined filter files) and dynamic (based on user-passed header) filtering of Spring REST api response.
For the same endpoint, we can set different filters per-clientId and use regular sync and async endpoints. 

Offcourse filtering completed json response is not as resource-efficient as just not serializing unwanted fields, but Jsonsiever is based on fast Jackson Streaming API and resource usage drawback should be generally acceptible.  

Lib is ready to use by just plugging it into Spring 3.X / Java 17 application. Also, its code might be relatively easily adapted to Spring 2.X / Java 8. 
It is also possible to plug lib into non-Spring or even entirely non-web application (see below [How to plug Jsonsiever lib into any (Java 17) application](#How-to-plug-Jsonsiever-lib-into-any-(Java-17)-application)) and just use core `JsonFilteringService` class to filter json in other scenarions, besides processing REST responses. This class (as well as whole `jsonsiever.core` package it depends on) is also ready to be compiled with Java 8.

## How to plug Jsonsiever lib into Spring 3.X application
There is a simple demo Spring application inside this repository - feel free to build it and play around - `jsonsiever/demo_app_spring`  

#### Step 1: Adjust your Spring configuration
```
@Configuration
@Import(JsonFilteringConfig.class)               //JSONSIEVER - import Jsonsiever config
@EnableWebMvc
public class AppConfig {
                                                 //JSONSIEVER - declare Jsonsiever servlet filter
    @Bean
    public FilterRegistrationBean<JsonResponseBodyFilter> filterRegistrationBean(
            JsonResponseFilterApplier jsonResponseFilterApplier
    ) {
        FilterRegistrationBean<JsonResponseBodyFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(
                new JsonResponseBodyFilter(jsonResponseFilterApplier)
        );

        registrationBean.addUrlPatterns("/*");   //JSONSIEVER - you may want to allow filter only for some base path
        registrationBean.setOrder(99);           //JSONSIEVER - you may want to adjust filter order but not mandatory
        return registrationBean;
    }

                                                 //JSONSIEVER - you may want to provide your own implementation for this cache
                                                 //(used to store header-based filters, if you are going to use them)
                                                 //See javadoc for SimpleInMemoryDynamicJsonFiltersCacheWrapper)
    @Bean                                            
    public DynamicJsonFiltersCacheWrapper simpleInMemoryDynamicJsonFiltersCacheWrapper() {
        return new SimpleInMemoryDynamicJsonFiltersCacheWrapper();
    }
}
```

#### Step 2: create filtering settings file
Create file `json-filtering-settings.yml` inside classpath (e.g. `resources` folder) and configure your endpoints (see javadoc for `JsonFilteringSettings`)
Example from `demo_app_spring`:
```
filterHeaderName: "X-json-filter-pattern"
clientIdHeaderName: "X-client-id"
endpoints:
  - path: "GET_/get-cats"
    regexpPath: false
    headerPatternAllowed: true
    filePatternAllowed: true
    filePatternPathPerClient:
      our-mobile-app: "/json-filters/GET_get-cats/our-mobile-app.json"
      default: "/json-filters/GET_get-cats/default.json"
  - path: "POST_/activate-cat/.+"
    regexpPath: true
    headerPatternAllowed: true
    filePatternAllowed: true
    filePatternPathPerClient:
      our-mobile-app: "/json-filters/POST_activate-cat/our-mobile-app.json"
      default: "/json-filters/POST_activate-cat/default.json"
```

#### Step 3: add filter files for your endpoints (if you are going to use file-based filters)
Create dir `json-filters` inside classpath (e.g. `resources` folder) and inside it - directories for each configured endpoint.  
Indide each per-endpoint directory you may have 1 or more per-client filter files.  
E.g. in abov example config - we have 2 endpoints, each with defautlt filter and with separate filter for client-id `our-mobile-app` which can be passed by client in HTTP header.  
Example from `demo_app_spring` goes as following structure:
```
json-filters/
  GET_get-cats/
    default.json
    our-mobile-app.json
  POST_activate-cat/
    default.json
    our-mobile-app.json
```

Content of filter files is WYSIWYG json filtering pattern (mask). All details on filter pattern composition are available inside javadoc for `JsonFilteringService`.  
Simple example from `demo_app_spring` file `demo_app_spring/src/main/resources/json-filters/POST_activate-cat/our-mobile-app.json`:
```
{
  "stats": {
    "agi": 1
  }
}
```
(this filters out `str` field that otherwise would be present in response)  

### Usage
After above configuration, requests to configured endpoints will be filtered. Since we have both header-based and file-based filtering enabled, priority will be following:
1. if user provides `X-json-filter-pattern` header with valid filter pattern json - this filter is applied.
2. else, if user provides `X-client-id` header with value we created filters for (`our-mobile-app`) - client-specific filter is applied.
3. else - default client filter file will be used.
4. in case we didnt have nor header value neither any of filter files present for this endpoint, or in case any processing error happened - response would be returned as-is, without any filtering applied. 

## How to plug Jsonsiever lib into any (Java 17) application 
Without Spring lib would just require `Jackson` and `Slf4j` dependencies added to your application and thats it - `JsonFilteringService` will be usable directly from your code.  

Example Gradle dependencies:
```
dependencies {
    implementation files('libs/jsonsiever-0.1.0.jar')
 
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.1'
    implementation 'org.slf4j:slf4j-api:1.7.25'
}
```
Usage:  
```
ObjectMapper objectMapper = new ObjectMapper();
JsonFilteringService filteringService = new JsonFilteringService(objectMapper.getFactory());

String jsonData = "{\"field1\": 123, \"obj\": {\"f1\": 123, \"f2\": 456}}";
JsonNode filterPattern = objectMapper.readTree("{\"obj\": {\"f1\": 123}}");

System.out.println(
    new String(filteringService.filterJsonFields(jsonData.getBytes(), filterPattern))
);
```

## How to use Jsonsiever with any Java 8+ application
You could just take source for whole `jsonsiever.core` package (just a few classes besides `JsonFilteringService`) and compile it with Java 8, adding some version of Jackson and Slf4 as dependency.
Then use `JsonFilteringService` the same way as in [How to plug Jsonsiever lib into any (Java 17) application](#How-to-plug-Jsonsiever-lib-into-any-(Java-17)-application)
