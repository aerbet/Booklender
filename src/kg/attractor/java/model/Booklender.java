package kg.attractor.java.model;

import kg.attractor.java.utils.FileUtil;

import java.util.*;

public class Booklender {
    private BooklenderData booklenderData;
    private final Map<String, Employee> usersMap = new HashMap<>();

    public Booklender() {
        loadData();
    }

    private void loadData() {
        this.booklenderData = FileUtil.readData();
        usersMap.clear();
        if (booklenderData.getEmployees() != null) {
            for (Employee e : booklenderData.getEmployees()) {
                usersMap.put(e.getId(), e);
            }
        }
    }

    public boolean register(String email, String password, String firstName) {
        if (usersMap.containsKey(email)) {
            return false;
        }

        Employee newEmployee = new Employee(email, firstName, password, "", "Новый сотрудник");

        booklenderData.getEmployees().add(newEmployee);
        usersMap.put(email, newEmployee);

        FileUtil.saveData(booklenderData);

        return true;
    }

    public Employee login(String email, String password) {
        Employee found = usersMap.get(email);
        if (found != null && found.getPassword().equals(password)) {
            return found;
        }
        return null;
    }

    public List<Book> getBooksList() {
        return booklenderData.getBooks();
    }

    public Book findBookById(int id) {
        return booklenderData.getBooks().stream()
                .filter(b -> b.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<Employee> getAllEmployees() {
        return booklenderData.getEmployees();
    }

    public Map<Employee, List<Book>> getCurrentBooksMap() {
        Map<Employee, List<Book>> map = new HashMap<>();
        Map<String, List<Integer>> recordsJson = booklenderData.getRecords();

        for (Employee e : getAllEmployees()) {
            List<Integer> bookIds = recordsJson.getOrDefault(e.getId(), List.of());
            List<Book> books = bookIds.stream()
                    .map(this::findBookById)
                    .filter(Objects::nonNull)
                    .toList();
            map.put(e, books);
        }
        return map;
    }

    public Map<String, List<Integer>> getRecordsJson() {
        return booklenderData.getRecords();
    }
}