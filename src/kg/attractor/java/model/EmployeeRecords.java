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

  public void setCurrentBooks(List<Book> currentBooks) {
    this.currentBooks = currentBooks;
  }

  public void setPreviousBooks(List<Book> previousBooks) {
    this.previousBooks = previousBooks;
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
