package kg.attractor.java.model;
import java.time.LocalDateTime;

public class Book {
    private int id;
    private String name;
    private String description;
    private String status;
    private String author;
    private LocalDateTime issueDate;
    private LocalDateTime returnDate;
    private String imageUrl;

    public Book() {
    }
}
