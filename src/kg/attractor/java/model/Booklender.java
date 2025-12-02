package kg.attractor.java.model;

import kg.attractor.java.utils.FileUtil;

import java.util.*;

public class Booklender {
  private BooklenderData booklenderData;
  private final Map<String, Employee> usersMap = new HashMap<>();

  public Booklender() {
    loadData();
  }

  public BooklenderData getBooklenderData() {
    return booklenderData;
  }

  public void setBooklenderData(BooklenderData booklenderData) {
    this.booklenderData = booklenderData;
  }

  public Map<String, Employee> getUsersMap() {
    return usersMap;
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

  public List<EmployeeRecordData> getEmployeeRecordsTemplate() {
    List<EmployeeRecordData> employeeRecordsList = new ArrayList<>();
    Map<String, EmployeeRecords> recordsMap = booklenderData.getEmployeeRecords();

    for (Employee employee : getAllEmployees()) {
      EmployeeRecords records = recordsMap.getOrDefault(employee.getId(), new EmployeeRecords());
      EmployeeRecordData recordData = new EmployeeRecordData(employee, records);
      employeeRecordsList.add(recordData);
    }

    return employeeRecordsList;
  }

  public Map<String, EmployeeRecords> getEmployeeRecordsMap() {
    return booklenderData.getEmployeeRecords();
  }

  public EmployeeRecords getEmployeeRecordsForEmployee(String employeeId) {
    Map<String, EmployeeRecords> recordsMap = booklenderData.getEmployeeRecords();

    return recordsMap.getOrDefault(employeeId, new EmployeeRecords());
  }

  public boolean issueBook(String employeeId, int bookId) {
    Book book = findBookById(bookId);
    if (book == null || !book.getStatus().equals("Available")) {
      return false;
    }

    EmployeeRecords employeeRecords = getEmployeeRecordsForEmployee(employeeId);

    if (employeeRecords.getCurrentBooks().size() >= 2) {
      return false;
    }

    book.setStatus(employeeId);
    employeeRecords.getCurrentBooks().add(book);
    booklenderData.getEmployeeRecords().put(employeeId, employeeRecords);
    FileUtil.saveData(booklenderData);

    return true;
  }

  public boolean returnBook(String employeeId, int bookId) {
    Book book = findBookById(bookId);
    if (book == null || !book.getStatus().equals(employeeId)) {
      return false;
    }

    EmployeeRecords employeeRecords = getEmployeeRecordsForEmployee(employeeId);
    boolean removed = employeeRecords.getCurrentBooks().removeIf(b -> b.getId() == book.getId());

    if (removed) {
      boolean alreadyPreviousBook = employeeRecords.getPreviousBooks().stream()
              .anyMatch(b -> b.getId() == book.getId());

      if (!alreadyPreviousBook) {
        employeeRecords.getPreviousBooks().add(book);
      }

      book.setStatus("Available");
      FileUtil.saveData(booklenderData);
      return true;
    }

    return false;
  }
}