package kg.attractor.java.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import kg.attractor.java.model.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public class BasicServer {
  private final static Configuration freemarker = initFreeMarker();
  private Map<String, RouteHandler> routes = new HashMap<>();
  private final String dataDir = "data";
  private final Booklender booklender;
  private final HttpServer server;

  private final Map<String, Employee> sessions = new HashMap<>();
  private final static String SESSION_COOKIE_NAME = "sessionId";
  private final static int SESSION_MAX_AGE_SECONDS = 600;

  public BasicServer(String host, int port) throws IOException {
    server = createServer(host, port);
    registerCommonHandlers();
    this.booklender = new Booklender();

    registerGet("/books", this::booksHandler);
    registerGet("/book", this::bookHandler);
    registerGet("/employee", this::employeeHandler);
    registerGet("/login", this::loginHandler);
    registerGet("/register", this::registerHandler);
    registerGet("/profile", this::profileHandler);
    registerGet("/logout", this::logoutHandler);

    registerPost("/login", this::loginPostHandler);
    registerPost("/register", this::registerPostHandler);
    registerPost("/issue", this::issueBookHandler);
    registerPost("/return", this::returnBookHandler);
  }

  private void logoutHandler(HttpExchange exchange) {
    String cookieString = getCookies(exchange);
    Map<String, String> cookies = Cookie.parse(cookieString);
    String sessionId = cookies.get(SESSION_COOKIE_NAME);

    if (sessionId != null) {
      sessions.remove(sessionId);

      Cookie<String> sessionCookie = new Cookie<>(SESSION_COOKIE_NAME, sessionId);
      sessionCookie.setMaxAge(0);
      setCookie(exchange, sessionCookie);
    }

    exchange.getResponseHeaders().set("Location", "/login");
    try {
      exchange.sendResponseHeaders(ResponseCodes.SEE_OTHER.getCode(), -1);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private void issueBookHandler(HttpExchange exchange) {
    Employee user = getAuthenticatedUser(exchange);

    if (user == null || !exchange.getRequestMethod().equals("POST")) {
      exchange.getResponseHeaders().set("Location", "/login");
      try {
        exchange.sendResponseHeaders(ResponseCodes.SEE_OTHER.getCode(), -1);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
      return;
    }

    String body = getRequestBody(exchange);
    Map<String, String> parsed = Utils.parseUrlEncoded(body, "&");

    String bookIdString = parsed.get("bookId");
    int bookId = Integer.parseInt(bookIdString);

    boolean success = booklender.issueBook(user.getId(), bookId);

    exchange.getResponseHeaders().set("Location", "/book?id=" + bookId);
    try {
      exchange.sendResponseHeaders(ResponseCodes.SEE_OTHER.getCode(), -1);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private void returnBookHandler(HttpExchange exchange) {
    Employee user = getAuthenticatedUser(exchange);

    if (user == null || !exchange.getRequestMethod().equals("POST")) {
      exchange.getResponseHeaders().set("Location", "/login");
      try {
        exchange.sendResponseHeaders(ResponseCodes.SEE_OTHER.getCode(), -1);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
      return;
    }

    String body = getRequestBody(exchange);
    Map<String, String> parsed = Utils.parseUrlEncoded(body, "&");

    String bookIdString = parsed.get("bookId");
    int bookId = Integer.parseInt(bookIdString);

    boolean success = booklender.returnBook(user.getId(), bookId);

    exchange.getResponseHeaders().set("Location", "/book?id=" + bookId);
    try {
      exchange.sendResponseHeaders(ResponseCodes.SEE_OTHER.getCode(), -1);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private void profileHandler(HttpExchange exchange) {
    Employee employee = getAuthenticatedUser(exchange);

    if (employee == null) {
      exchange.getResponseHeaders().set("Location", "/login");
      try {
        exchange.sendResponseHeaders(303, -1);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }

    Map<String, Object> data = new HashMap<>();
    data.put("employee", employee);

    EmployeeRecords records = booklender.getEmployeeRecordsForEmployee(employee.getId());
    data.put("records", records);

    renderTemplate(exchange, "profile.html", data);
  }

  private void registerHandler(HttpExchange exchange) {
    renderTemplate(exchange, "register.html", null);
  }

  private void registerPostHandler(HttpExchange exchange) {
    String body = getRequestBody(exchange);
    Map<String, String> parsed = Utils.parseUrlEncoded(body, "&");

    String email = parsed.get("email");
    String password = parsed.get("password");
    String firstName = parsed.get("name");

    try {
      if (email == null || password == null || firstName == null || email.isEmpty() || password.isEmpty() || firstName.isEmpty()) {
        sendByteData(exchange, ResponseCodes.BAD_REQUEST, ContentType.TEXT_PLAIN, "Заполните все поля!".getBytes());
        return;
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    boolean success = booklender.register(email, password, firstName);

    if (success) {
      Employee user = booklender.login(email, password);

      Map<String, Object> data = new HashMap<>();
      data.put("employee", user);

      Map<String, Object> dummyRecords = new HashMap<>();
      dummyRecords.put("currentBooks", new java.util.ArrayList<>());
      dummyRecords.put("previousBooks", new java.util.ArrayList<>());
      data.put("records", dummyRecords);

      renderTemplate(exchange, "login.html", data);
    } else {
      Map<String, Object> data = new HashMap<>();
      data.put("error", "Пользователь с таким email уже существует!");
      renderTemplate(exchange, "registerFail.html", data);
    }
  }

  private void loginHandler(HttpExchange exchange) {
    renderTemplate(exchange, "login.html", null);
  }

  private void loginPostHandler(HttpExchange exchange) {
    String body = getRequestBody(exchange);
    Map<String, String> parsed = Utils.parseUrlEncoded(body, "&");

    String login = parsed.get("email");
    String password = parsed.get("user-password");

    Employee user = booklender.login(login, password);

    if (user != null) {
      String sessionId = UUID.randomUUID().toString();
      sessions.put(sessionId, user);
      Cookie<String> sessionCookie = new Cookie<>(SESSION_COOKIE_NAME, sessionId);
      sessionCookie.setMaxAge(SESSION_MAX_AGE_SECONDS);
      sessionCookie.setHttpOnly(true);
      setCookie(exchange, sessionCookie);

      Map<String, Object> data = new HashMap<>();
      data.put("employee", user);

      Map<String, Object> dummyRecords = new HashMap<>();
      dummyRecords.put("currentBooks", new java.util.ArrayList<>());
      dummyRecords.put("previousBooks", new java.util.ArrayList<>());
      data.put("records", dummyRecords);

      exchange.getResponseHeaders().set("Location", "/profile");
      try {
        exchange.sendResponseHeaders(ResponseCodes.SEE_OTHER.getCode(), -1);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    } else {
      Map<String, Object> data = new HashMap<>();
      data.put("error", "Неверный логин или пароль");
      renderTemplate(exchange, "login.html", data);
    }
  }

  private void employeeHandler(HttpExchange exchange) {
    try {
      String query = exchange.getRequestURI().getQuery();
      Map<String, String> params = Utils.parseUrlEncoded(query, "&");
      String employeeId = params.get("id");

      Booklender lender = getBooklender();
      Employee found = lender.getUsersMap().get(employeeId);

      if (found == null) {
        respond404(exchange);
        return;
      }

      EmployeeRecords empRecords = lender.getEmployeeRecordsForEmployee(found.getId());

      Map<String, Object> model = new HashMap<>();
      model.put("employee", found);
      model.put("records", empRecords);

      renderTemplate(exchange, "employee.html", model);
    } catch (Exception e) {
      e.printStackTrace();
      try {
        sendByteData(exchange, ResponseCodes.SERVER_ERROR, ContentType.TEXT_PLAIN, "Server error".getBytes());
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  private void bookHandler(HttpExchange exchange) {
    try {
      String query = exchange.getRequestURI().getQuery();
      Map<String, String> params = Utils.parseUrlEncoded(query, "&");
      String idString = params.get("id");
      int bookId = -1;

      if (idString != null) {
        bookId = Integer.parseInt(idString);
      }

      Booklender lender = getBooklender();
      Book book = lender.findBookById(bookId);

      if (book == null) {
        respond404(exchange);
        return;
      }

      Employee currentUser = getAuthenticatedUser(exchange);

      int currentBooksCount = 0;
      if (currentUser != null) {
        EmployeeRecords userRecords = lender.getEmployeeRecordsForEmployee(currentUser.getId());
        currentBooksCount = userRecords.getCurrentBooks().size();
      }

      Employee holder = null;
      if (!book.getStatus().equals("Available")) {
        String holderId = book.getStatus();
        holder = lender.getUsersMap().get(holderId);
      }

      Map<String, Object> model = new HashMap<>();
      model.put("book", book);
      model.put("currentUser", currentUser);
      model.put("holder", holder);
      model.put("currentBooksCount", currentBooksCount);

      renderTemplate(exchange, "book.html", model);
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
  }

  private void booksHandler(HttpExchange exchange) {
    Map<String, Object> model = new HashMap<>();

    model.put("employeeRecords", booklender.getEmployeeRecordsTemplate());
    model.put("usersMap", booklender.getUsersMap());
    model.put("employeeRecordsMap", booklender.getEmployeeRecordsMap());
    model.put("booksList", booklender.getBooksList());
    renderTemplate(exchange, "books.html", model);
  }

  private static Configuration initFreeMarker() {
    try {
      Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
      cfg.setDirectoryForTemplateLoading(new File("data"));

      cfg.setDefaultEncoding("UTF-8");
      cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      cfg.setLogTemplateExceptions(false);
      cfg.setWrapUncheckedExceptions(true);
      cfg.setFallbackOnNullLoopVariable(false);
      return cfg;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void renderTemplate(HttpExchange exchange, String templateFile, Object dataModel) {
    try {
      Template temp = freemarker.getTemplate(templateFile);

      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (OutputStreamWriter writer = new OutputStreamWriter(stream)) {

        temp.process(dataModel, writer);
        writer.flush();

        var data = stream.toByteArray();

        sendByteData(exchange, ResponseCodes.OK, ContentType.TEXT_HTML, data);
      }
    } catch (IOException | TemplateException e) {
      e.printStackTrace();
    }
  }

  private Booklender getBooklender() {
    return booklender;
  }

  protected static String getContentType(HttpExchange exchange) {
    return exchange.getRequestHeaders()
            .getOrDefault("Content-Type", List.of(""))
            .getFirst();
  }

  protected static String getRequestBody(HttpExchange exchange) {
    InputStream stream = exchange.getRequestBody();
    Charset charset = StandardCharsets.UTF_8;
    InputStreamReader isr = new InputStreamReader(stream, charset);

    try (BufferedReader br = new BufferedReader(isr)) {
      return br.lines().collect(Collectors.joining(""));
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return "";
  }

  protected void setCookie(HttpExchange exchange, Cookie cookie) {
    exchange.getResponseHeaders().add("Set-Cookie", cookie.toString());
  }

  protected static String getCookies(HttpExchange exchange) {
    return exchange.getRequestHeaders()
            .getOrDefault("Cookie", List.of(""))
            .getFirst();

  }

  protected Employee getAuthenticatedUser(HttpExchange exchange) {
    String cookieString = getCookies(exchange);
    Map<String, String> cookies = Cookie.parse(cookieString);
    String sessionId = cookies.get(SESSION_COOKIE_NAME);

    if (sessionId == null) {
      return null;
    }

    return sessions.get(sessionId);
  }

  private static String makeKey(String method, String route) {
    return String.format("%s %s", method.toUpperCase(), route);
  }

  private static String makeKey(HttpExchange exchange) {
    var method = exchange.getRequestMethod();
    var path = exchange.getRequestURI().getPath();

    var index = path.lastIndexOf(".");
    var extOrPath = index != -1 ? path.substring(index).toLowerCase() : path;

    return makeKey(method, extOrPath);
  }

  private static void setContentType(HttpExchange exchange, ContentType type) {
    exchange.getResponseHeaders().set("Content-Type", String.valueOf(type));
  }

  private static HttpServer createServer(String host, int port) throws IOException {
    var msg = "Starting server on http://%s:%s/%n";
    System.out.printf(msg, host, port);
    var address = new InetSocketAddress(host, port);
    return HttpServer.create(address, 50);
  }

  private void registerCommonHandlers() {
    server.createContext("/", this::handleIncomingServerRequests);

    registerGet("/", exchange -> sendFile(exchange, makeFilePath("index.html"), ContentType.TEXT_HTML));

    registerFileHandler(".css", ContentType.TEXT_CSS);
    registerFileHandler(".html", ContentType.TEXT_HTML);
    registerFileHandler(".jpg", ContentType.IMAGE_JPEG);
    registerFileHandler(".png", ContentType.IMAGE_PNG);

  }

  protected final void registerGet(String route, RouteHandler handler) {
    getRoutes().put("GET " + route, handler);
  }

  protected final void registerPost(String route, RouteHandler handler) {
    getRoutes().put("POST " + route, handler);
  }

  protected final void registerFileHandler(String fileExt, ContentType type) {
    registerGet(fileExt, exchange -> sendFile(exchange, makeFilePath(exchange), type));
  }

  protected final Map<String, RouteHandler> getRoutes() {
    return routes;
  }

  protected final void sendFile(HttpExchange exchange, Path pathToFile, ContentType contentType) {
    try {
      if (Files.notExists(pathToFile)) {
        respond404(exchange);
        return;
      }
      var data = Files.readAllBytes(pathToFile);
      sendByteData(exchange, ResponseCodes.OK, contentType, data);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Path makeFilePath(HttpExchange exchange) {
    return makeFilePath(exchange.getRequestURI().getPath());
  }

  protected Path makeFilePath(String... s) {
    return Path.of(dataDir, s);
  }

  protected final void sendByteData(HttpExchange exchange, ResponseCodes responseCode,
                                    ContentType contentType, byte[] data) throws IOException {
    try (var output = exchange.getResponseBody()) {
      setContentType(exchange, contentType);
      exchange.sendResponseHeaders(responseCode.getCode(), 0);
      output.write(data);
      output.flush();
    }
  }

  private void respond404(HttpExchange exchange) {
    try {
      var data = "404 Not found".getBytes();
      sendByteData(exchange, ResponseCodes.NOT_FOUND, ContentType.TEXT_PLAIN, data);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handleIncomingServerRequests(HttpExchange exchange) {
    var route = getRoutes().getOrDefault(makeKey(exchange), this::respond404);
    route.handle(exchange);
  }

  public final void start() {
    server.start();
  }
}
