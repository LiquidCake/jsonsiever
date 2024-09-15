# jsonsiever
Dynamic Json filtering library for Java/Spring REST APIs 
This lib will be useful in a situation when we want to filter (potentially large and complex) json object using kind of WYSIWYG filtering pattern.  

E.g. we have following json and want to remove `stats.agi` field:
```
{
  "name": "orange",
  "stats": {
    "str": 8,
    "agi": 10
  }
}
```
Then filter would look like this:
```
{
  "name": 1,
  "stats": {
    "str": 1
  }
}
```
Filter that removes `name` field while keeping `stats` object intact would look like this:
```
{
  "stats": {}
}
```
(more examples in javadoc of [JsonFilteringService](https://www.javadoc.io/doc/io.github.liquidcake/jsonsiever/latest/io/github/liquidcake/jsonsiever/core/JsonFilteringService.html))

## Main use-case:
Let's assume we have a Spring REST API with some heavy endpoint, and we want to remove particular deeply-nested fields from the response, to make it lighter and more easily parsable for clients.
Sometimes we can change our response structure, prevent population of some fields etc. But this is not always an option.  
Another possible solution would be to wrap our endpoint in a GraphQL query and let user chose desired fields for each request. But this approach also has its downsides.  

This library is meant to be an "easy to plug in" configurable alternative that allows both static (based on pre-defined file-based filter) and dynamic (based on user-passed header) filtering of json response for Spring HTTP endpoints.
It supports setting different filters per-clientId for the same endpoint and usage of both regular sync and async endpoints. 

Of course, filtering a complete json response is not as resource-efficient as just not serializing unwanted fields at the first place, but Jsonsiever is based on fast Jackson Streaming API and resource usage drawback should be generally acceptable.  

Lib is ready to use by just plugging it into SpringBoot 3.X / Java 17 application. Also, its code might be relatively easily adapted to SpringBoot 2.X / Java 8 or plain Spring / Java Servlet app.  
It is also possible to plug the lib into plain Java 17 application and use it for manual json filtering (see below [How to use Jsonsiever lib with plain Java application](#How-to-use-Jsonsiever-lib-with-plain-Java-application)). In this case one would just use core `JsonFilteringService` class to filter json in other scenarios, besides processing REST responses. This class (as well as whole `io.github.liquidcake.jsonsiever.core` package it is part of) is ready to be compiled with Java 8.

## Documentation
Details on filters structure are in javadoc for [JsonFilteringService](https://www.javadoc.io/doc/io.github.liquidcake/jsonsiever/latest/io/github/liquidcake/jsonsiever/core/JsonFilteringService.html) class  
Details on settings file are in javadoc for [JsonFilteringSettings](https://www.javadoc.io/doc/io.github.liquidcake/jsonsiever/latest/io/github/liquidcake/jsonsiever/web/config/JsonFilteringSettings.html) class  

## How to plug Jsonsiever lib into SpringBoot 3.X application
There is a simple demo SpringBoot application inside this repository - feel free to build it and play around - `jsonsiever/demo_app_spring`  

#### Step 1: Add dependency:
Maven:
```
<dependency>
    <groupId>io.github.liquidcake</groupId>
    <artifactId>jsonsiever</artifactId>
    <version>1.0.0</version>
</dependency>
```
Gradle:
```
implementation 'io.github.liquidcake:jsonsiever:1.0.0'
```

#### Step 2: Adjust your Spring configuration
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

#### Step 3: create filtering settings file
Create file `json-filtering-settings.yml` inside classpath (e.g. `resources` folder) and configure your endpoints (details in javadoc for [JsonFilteringSettings](https://www.javadoc.io/doc/io.github.liquidcake/jsonsiever/latest/io/github/liquidcake/jsonsiever/web/config/JsonFilteringSettings.html))  
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

#### Step 4: add filter files for your endpoints (if you are going to use file-based filters)
Create dir `json-filters` inside classpath (e.g. `resources` folder) and inside it - directories for each configured endpoint.  
Inside each per-endpoint directory you may have 1 or more per-client filter files.  
E.g. in above example config - we have 2 endpoints, each with default filter and with separate filter for client-id `our-mobile-app` which can be passed by client in HTTP header.  
Example from `demo_app_spring` has the following structure:
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
(this filters out `stats.str` field that otherwise would be present in response)  

### Usage
After making above configuration - requests to configured endpoints will be filtered. Since we have both header-based and file-based filtering enabled, priority will be following:
1. if user provides `X-json-filter-pattern` header with valid filter pattern json - this filter is applied.
2. else, if user provides `X-client-id` header with a value we have filters for (e.g. `our-mobile-app`) - client-specific filter is applied.
3. else - default client filter file will be used (since we have it in this example).
4. in case we didn't have nor header value neither any of filter files present for this endpoint, or in case any processing error happened - response would be returned as-is, without any filtering applied. 

Example CURL requests (with both headers enabled):
```
curl --location '127.0.0.1:8080/get-cats' \
--header 'x-client-id: our-mobile-app' \
--header 'X-json-filter-pattern: [{"name": 1}]'

curl --location --request POST '127.0.0.1:8080/activate-cat/orange?fakeParam=blabla' \
--header 'x-client-id: our-mobile-app' \
--header 'X-json-filter-pattern: {"name": 1}'

```

## How to use Jsonsiever lib with plain Java application
### Java 17
Without SpringBoot, lib would just require `Jackson` and `Slf4j` dependencies added explicitly to your application and that's it - `JsonFilteringService` will be usable directly from your code.  

Example Gradle dependencies:
```
dependencies {
    implementation 'io.github.liquidcake:jsonsiever:1.0.0'
 
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

byte[] filteredJson = filteringService.filterJsonFields(jsonData.getBytes(), filterPattern);
```

### Java 8+
Lib is compiled with Java 17 but core sources are Java 8 compatible.  
You could just manually take a source for whole `io.github.liquidcake.jsonsiever.core` package (just a few classes besides `JsonFilteringService`) and compile it with Java 8, adding some version of Jackson and Slf4 as dependency.  
Then use `JsonFilteringService` the same way as in [Java 17](#Java-17)
