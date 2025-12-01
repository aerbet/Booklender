package kg.attractor.java.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import kg.attractor.java.model.SampleDataModel;
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

    private final HttpServer server;
    private final String dataDir = "data";
    private Map<String, RouteHandler> routes = new HashMap<>();
    private final static Configuration freemarker = initFreeMarker();
    private final Booklender booklender;

    public BasicServer(String host, int port) throws IOException {
        server = createServer(host, port);
        registerCommonHandlers();
        this.booklender = new Booklender();

        registerGet("/sample", this::freemarkerSampleHandler);
        registerGet("/books", this::freemarkerBooksHandler);
        registerGet("/book", this::freemarkerBookHandler);
        registerGet("/employees", this::freemarkerEmployeesHandler);
        registerGet("/employee", this::freemarkerEmployeeHandler);
        registerGet("/login", this::loginHandler);
        registerGet("/register", this::registerHandler);
        registerGet("/profile", this::profileHandler);

        registerPost("/login", this::loginPostHandler);
        registerPost("/register", this::registerPostHandler);
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
        // самый основной обработчик, который будет определять
        // какие обработчики вызывать в дальнейшем
        server.createContext("/", this::handleIncomingServerRequests);

        // специфичные обработчики, которые выполняют свои действия
        // в зависимости от типа запроса

        // обработчик для корневого запроса
        // именно этот обработчик отвечает что отображать,
        // когда пользователь запрашивает localhost:9889
        registerGet("/", exchange -> sendFile(exchange, makeFilePath("index.html"), ContentType.TEXT_HTML));

        // эти обрабатывают запросы с указанными расширениями
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

    private void profileHandler(HttpExchange exchange) {
        Map<String, Object> data = new HashMap<>();
        Employee employee = null;

        if (employee == null) {
            Employee dummy = new Employee("guest@example.com", "Некий", "none", "Пользователь", "No position");

            data.put("employee", dummy);
            data.put("records", new HashMap<>());
        } else {
            data.put("employee", employee);
            data.put("records", new HashMap<>());
        }

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
            Map<String, Object> data = new HashMap<>();

            data.put("employee", user);

            Map<String, Object> dummyRecords = new HashMap<>();
            dummyRecords.put("currentBooks", new java.util.ArrayList<>());
            dummyRecords.put("previousBooks", new java.util.ArrayList<>());
            data.put("records", dummyRecords);

            renderTemplate(exchange, "profile.html", data);

        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("error", "Неверный логин или пароль");
            renderTemplate(exchange, "login.html", data);
        }
    }

    private void freemarkerEmployeeHandler(HttpExchange exchange) {
        try {
            String query = exchange.getRequestURI().getQuery();
            int employeeId = -1;
            if (query != null && query.startsWith("id=")) {
                employeeId = Integer.parseInt(query.substring(3));
            }

            Booklender lender = getBooklender();
            Employee found = null;
            EmployeeRecords empRecords = null;
            if (found == null) {
                return;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("employee", found);
            model.put("records", empRecords);

            renderTemplate(exchange, "employee.html", model);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void freemarkerEmployeesHandler(HttpExchange exchange) {
        renderTemplate(exchange, "employees.html", getBooklender());
    }

    private void freemarkerBookHandler(HttpExchange exchange) {
        try {
            String query = exchange.getRequestURI().getQuery();
            int bookId = -1;

            if (query != null && query.startsWith("id=")) {
                bookId = Integer.parseInt(query.substring(3));
            }

            Booklender lender = getBooklender();
            Book book = lender.findBookById(bookId);

            Map<String, Object> model = new HashMap<>();
            model.put("book", book);
            model.put("lender", lender);

            renderTemplate(exchange, "book.html", model);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void freemarkerBooksHandler(HttpExchange exchange) {
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
            // путь к каталогу в котором у нас хранятся шаблоны
            // это может быть совершенно другой путь, чем тот, откуда сервер берёт файлы
            // которые отправляет пользователю
            cfg.setDirectoryForTemplateLoading(new File("data"));

            // прочие стандартные настройки о них читать тут
            // https://freemarker.apache.org/docs/pgui_quickstart_createconfiguration.html
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

    private void freemarkerSampleHandler(HttpExchange exchange) {
        renderTemplate(exchange, "sample.html", getSampleDataModel());
    }

    protected void renderTemplate(HttpExchange exchange, String templateFile, Object dataModel) {
        try {
            // Загружаем шаблон из файла по имени.
            // Шаблон должен находится по пути, указанном в конфигурации
            Template temp = freemarker.getTemplate(templateFile);

            // freemarker записывает преобразованный шаблон в объект класса writer
            // а наш сервер отправляет клиенту массивы байт
            // по этому нам надо сделать "мост" между этими двумя системами

            // создаём поток, который сохраняет всё, что в него будет записано в байтовый массив
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            // создаём объект, который умеет писать в поток и который подходит для freemarker
            try (OutputStreamWriter writer = new OutputStreamWriter(stream)) {

                // обрабатываем шаблон заполняя его данными из модели
                // и записываем результат в объект "записи"
                temp.process(dataModel, writer);
                writer.flush();

                // получаем байтовый поток
                var data = stream.toByteArray();

                // отправляем результат клиенту
                sendByteData(exchange, ResponseCodes.OK, ContentType.TEXT_HTML, data);
            }
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
        }
    }

    private Booklender getBooklender() {
        return booklender;
    }

    private SampleDataModel getSampleDataModel() {
        // возвращаем экземпляр тестовой модели-данных
        // которую freemarker будет использовать для наполнения шаблона
        return new SampleDataModel();
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
}
