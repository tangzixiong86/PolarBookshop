package com.polarbookshop.orderservice.book;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class BookClient {
    private static final String BOOKS_ROOT_API="/books/";
    private final WebClient webClient;
    public BookClient(WebClient webClient)
    {
        this.webClient=webClient;
    }
    public Mono<Book> getBookByIsbn(String isbn)
    {
        //将retryWhen() 操作符放在 timeout() 之后意味着超时应用于每次重试尝试。
        //将retryWhen() 操作符放在 timeout() 之前意味着超时应用于整个操作（即初始请求和重试的整个序列必须在给定的时间限制内发生）。
        return webClient
                .get()
                .uri(BOOKS_ROOT_API+isbn)
                .retrieve()
                .bodyToMono(Book.class)
                .timeout(Duration.ofSeconds(3),Mono.empty())
                .onErrorResume(WebClientResponseException.NotFound.class, exception  -> Mono.empty())
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                .onErrorResume(Exception.class, exception -> Mono.empty());
    }
}
