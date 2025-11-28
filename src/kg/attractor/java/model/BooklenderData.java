package kg.attractor.java.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BooklenderData {
    private List<Employee> employees = new ArrayList<>();
    private List<Book> books = new ArrayList<>();
    private Map<String, List<Integer>> records = new HashMap<>();

    public BooklenderData() {}

    public List<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }

    public Map<String, List<Integer>> getRecords() {
        return records;
    }

    public void setRecords(Map<String, List<Integer>> records) {
        this.records = records;
    }
}
