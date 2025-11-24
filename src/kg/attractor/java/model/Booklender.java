package kg.attractor.java.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Booklender {
    private Book book;
    private Employee employee;
    private List<Book> lendingRecords = new ArrayList<>();

    public Booklender() {
        this.book = new Book(1, "Harry Potter", "Lor", "Available", "Erbol", LocalDateTime.now(), LocalDateTime.now(), "img.jpg");
    }

    public Book getBook() {
        return book;
    }
    public void setBook(Book book) {}

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public List<Book> getLendingRecords() {
        return lendingRecords;
    }

    public void setLendingRecords(List<Book> lendingRecords) {
        this.lendingRecords = lendingRecords;
    }
}
