package kg.attractor.java.model;
import java.util.ArrayList;

public class Employee {
    private int id;
    private String login;
    private String password;
    private String firstName;
    private String lastName;
    private String position;
    private ArrayList<Book> currentBooks;
    private ArrayList<Book> previousBooks;

    public Employee() {
    }
}
