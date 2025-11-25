package kg.attractor.java.model;

import java.util.ArrayList;
import java.util.List;

public class EmployeeRecords {
    private List<Book> currentBooks = new ArrayList<>();
    private List<Book> previousBooks = new ArrayList<>();

    public EmployeeRecords() {
    }

    public List<Book> getCurrentBooks() {
        return currentBooks;
    }

    public List<Book> getPreviousBooks() {
        return previousBooks;
    }

    public void addCurrentBook(Book book) {
        currentBooks.add(book);
    }

    public void finishBook(Book book) {
        currentBooks.remove(book);
        previousBooks.add(book);
    }
}
