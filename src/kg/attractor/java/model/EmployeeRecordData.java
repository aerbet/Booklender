package kg.attractor.java.model;

public class EmployeeRecordData {
  private final Employee employee;
  private final EmployeeRecords records;

  public EmployeeRecordData(Employee employee, EmployeeRecords records) {
    this.employee = employee;
    this.records = records;
  }

  public Employee getEmployee() {
    return employee;
  }

  public EmployeeRecords getRecords() {
    return records;
  }
}
