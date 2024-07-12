package jsonsiever.demo;

import jakarta.annotation.PostConstruct;
import jsonsiever.demo.model.Cat;
import jsonsiever.demo.model.Stats;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class DemoController {

    private Map<String, Cat> cats;

    @PostConstruct
    void init() {
        cats = new HashMap<>();
        cats.put("orange",
                new Cat("orange",
                        new Stats(
                                8,
                                10
                        )
                )
        );
    }

    @GetMapping(value = "/get-cats")
    @ResponseBody
    public Collection<Cat> getCats() throws Exception {
        return cats.values();
    }

    @PostMapping(value = "/activate-cat/{name}")
    @ResponseBody
    public CompletableFuture<Cat> findCat(@PathVariable String name) throws Exception {

        return CompletableFuture.supplyAsync(() -> {
            Cat cat = cats.get(name);

            System.out.println("Meow * " + cat.getStats().getStr());

            return cat;
        });
    }
}