package me.wonwoo.springproxyfactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class SpringProxyFactoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringProxyFactoryApplication.class, args);
    }

    @HttpExchange("http://localhost:8080")
    interface GreetingClient {

        @GetExchange("/greeting")
        Flux<Greeting> greetings();

        @GetExchange("/greeting/{message}")
        Mono<Greeting> greetings(@PathVariable String message);

        @PostExchange("/greeting")
        Mono<Greeting> greetings(@RequestBody Greeting greeting);
    }

    @Bean
    GreetingClient gitHubClient(HttpServiceProxyFactory httpServiceProxyFactory) {
        return httpServiceProxyFactory.createClient(GreetingClient.class);
    }

    @Bean
    HttpServiceProxyFactory httpServiceProxyFactory(WebClient.Builder builder) {
        return new HttpServiceProxyFactory(new WebClientAdapter(builder.build()));
    }

    @EventListener
    public void start(ApplicationStartedEvent applicationStartedEvent) {
        GreetingClient greetingClient = gitHubClient(null);
        greetingClient.greetings(new Greeting("greeting"))
                .then(greetingClient.greetings("greeting"))
                .doOnNext(System.out::println)
                .flatMapMany(__ -> greetingClient.greetings())
                .subscribe(System.out::println);
    }

    record Greeting(String message) {
    }

    @RestController
    static class GreetingController {

        private final List<Greeting> greetings = new ArrayList<>(List.of(new Greeting("hello world")));

        @GetMapping("/greeting")
        public Flux<Greeting> greeting() {
            return Flux.fromIterable(greetings);
        }

        @GetMapping("/greeting/{message}")
        public Mono<Greeting> greeting(@PathVariable String message) {
            return Mono.justOrEmpty(greetings.stream().filter(it -> it.message.equals(message)).findFirst());
        }

        @PostMapping("/greeting")
        public Mono<Void> greeting(@RequestBody Greeting greeting) {
            greetings.add(greeting);
            return Mono.empty();
        }
    }
}
