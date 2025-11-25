package kg.attractor.java.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Booklender {
    private Map<Employee, EmployeeRecords> records = new HashMap<>();
    private List<Book> booksList = new ArrayList<>();

    public Booklender() {
        initData();
    }

    private void initData() {
        Book book1 = new Book(1, "Harry Potter", "Magic world", "Available", "J.K. Rowling", LocalDateTime.now(), LocalDateTime.now(), "https://placehold.co/300x450?text=Harry+Potter");
        Book book2 = new Book(2, "Lord of the Rings", "Epic fantasy", "Выдана", "J.R.R. Tolkien", LocalDateTime.now(), LocalDateTime.now(), "https://placehold.co/300x450?text=LOTR");
        Book book3 = new Book(3, "Java for Beginners", "Coding", "Available", "Herbert Schildt", LocalDateTime.now(), LocalDateTime.now(), "https://placehold.co/300x450?text=Java");
        booksList.add(book1);
        booksList.add(book2);
        booksList.add(book3);

        Employee admin = new Employee(1, "admin", "pass", "John", "Doe", "Boss");
        Employee junior = new Employee(2, "user", "pass", "Kevin", "Lee", "Junior Dev");
        records.put(admin, new EmployeeRecords());
        records.put(junior, new EmployeeRecords());

        EmployeeRecords adminRecords = records.get(admin);
        adminRecords.addCurrentBook(book1);
        adminRecords.finishBook(book1);

        EmployeeRecords juniorRecords = records.get(junior);
        juniorRecords.addCurrentBook(book2);
    }

    public Employee findEmployeeByCurrentBook(Book targetBook) {
        for (Map.Entry<Employee, EmployeeRecords> entry : records.entrySet()) {
            Employee employee = entry.getKey();
            EmployeeRecords empRecords = entry.getValue();

            if (empRecords.getCurrentBooks().contains(targetBook)) {
                return employee;
            }
        }
        return null;
    }

    public List<Book> getBooksList() {
        return booksList;
    }

    public void setBooksList(List<Book> booksList) {
        this.booksList = booksList;
    }

    public Map<Employee, EmployeeRecords> getRecords() {
        return records;
    }

    public void setRecords(Map<Employee, EmployeeRecords> records) {
        this.records = records;
    }
}