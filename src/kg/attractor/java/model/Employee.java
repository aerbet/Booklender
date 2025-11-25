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

    public Employee(int id, String login, String password, String firstName, String lastName, String position) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.currentBooks = new ArrayList<>(){};
        this.previousBooks = new ArrayList<>(){};
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public ArrayList<Book> getCurrentBooks() {
        return currentBooks;
    }

    public void setCurrentBooks(ArrayList<Book> currentBooks) {
        this.currentBooks = currentBooks;
    }

    public ArrayList<Book> getPreviousBooks() {
        return previousBooks;
    }

    public void setPreviousBooks(ArrayList<Book> previousBooks) {
        this.previousBooks = previousBooks;
    }
}
