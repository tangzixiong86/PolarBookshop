package com.polarbookshop.catalogservice.web;

import com.polarbookshop.catalogservice.domain.Book;
import com.polarbookshop.catalogservice.domain.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("books")
public class BookController {
    private final BookService bookService;
    public BookController(BookService bookService)
    {
        this.bookService = bookService;
    }
    @GetMapping
    public Iterable<Book> get(){
        return bookService.viewBookList();
    }
    @GetMapping("{isbn}")
    public Book getByIsbn(@PathVariable String isbn){
        return bookService.viewBookDetails(isbn);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Book> post(@Valid @RequestBody Book book){
        var newBook = bookService.addBookToCatalog(book);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{isbn}")
                .buildAndExpand(newBook.isbn()).toUri();
        return ResponseEntity.created(location).body(newBook);
    }
    @DeleteMapping("{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String isbn) {
        bookService.removeBookFromCatalog(isbn);
    }
    @PutMapping("{isbn}")
    public Book put(@PathVariable String isbn, @Valid @RequestBody Book book){
        return bookService.editBookDetails(isbn, book);
    }
}
