package kg.attractor.java.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Booklender {
    private Book book;
    private Employee employee;
    private List<Book> lendingRecords = new ArrayList<>();

    public Booklender() {
        this.employee = new Employee(1, "admin", "admin", "John", "Doe", "boss");
        this.lendingRecords = new ArrayList<>();

        this.lendingRecords.add(new Book(
                1,
                "Harry Potter",
                "J.K. Rowling",
                "Available",
                "Fantasy",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "img1.jpg"));
        this.lendingRecords.add(new Book(
                2,
                "Lord of the Rings",
                "J.R.R. Tolkien",
                "Выдана admin",
                "Fantasy",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "img2.jpg"));
        this.lendingRecords.add(new Book(
                3,
                "Java for Beginners",
                "Herbert Schildt",
                "Available",
                "Education",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "img3.jpg"));

        this.book = this.lendingRecords.get(0);
    }

    public Book getBook() {
        return book;
    }
    public void setBook(Book book) {
        this.book = book;
    }

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
