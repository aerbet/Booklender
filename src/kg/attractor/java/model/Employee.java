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

    public Employee(int id, String login, String password, String firstName, String lastName, String position, ArrayList<Book> currentBooks, ArrayList<Book> previousBooks) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.currentBooks = new ArrayList<>(){};
        this.previousBooks = previousBooks;
    }
}
