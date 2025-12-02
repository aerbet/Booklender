package kg.attractor.java.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Book {
  private int id;
  private String title;
  private String description;
  private String status;
  private String author;
  private LocalDateTime issueDate;
  private LocalDateTime returnDate;
  private String image;

  public Book() {
  }

  public Book(int id, String name, String description, String status, String author, LocalDateTime issueDate, LocalDateTime returnDate, String imageUrl) {
    this.id = id;
    this.title = name;
    this.description = description;
    this.status = status;
    this.author = author;
    this.issueDate = issueDate;
    this.returnDate = returnDate;
    this.image = imageUrl;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public LocalDateTime getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(LocalDateTime issueDate) {
    this.issueDate = issueDate;
  }

  public LocalDateTime getReturnDate() {
    return returnDate;
  }

  public void setReturnDate(LocalDateTime returnDate) {
    this.returnDate = returnDate;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Book book = (Book) o;
    return id == book.id && Objects.equals(title, book.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title);
  }
}