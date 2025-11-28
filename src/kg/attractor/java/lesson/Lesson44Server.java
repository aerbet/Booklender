package kg.attractor.java.lesson;

import com.sun.net.httpserver.HttpExchange;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import kg.attractor.java.model.*;
import kg.attractor.java.server.BasicServer;
import kg.attractor.java.server.ContentType;
import kg.attractor.java.server.ResponseCodes;
import kg.attractor.java.server.Utils;

import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Lesson44Server extends BasicServer {
    private final static Configuration freemarker = initFreeMarker();
    private final Booklender booklender;

    public Lesson44Server(String host, int port) throws IOException {
        super(host, port);
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
//            for (Map.Entry<Employee, EmployeeRecords> entry : lender.getBooksList()) {
//                if (entry.getKey().getId() == employeeId) {
//                    found = entry.getKey();
//                    empRecords = entry.getValue();
//                    break;
//                }
//            }

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
        model.put("booksList", booklender.getBooksList());
        model.put("records", prepareRecords(booklender));
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

    private Map<String, Map<String, Object>> prepareRecords(Booklender lender) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        Map<String, List<Integer>> recordsJson = lender.getRecordsJson();

        for (Employee e : lender.getAllEmployees()) {
            EmployeeRecords rec = new EmployeeRecords();

            List<Book> currentBooks = recordsJson.getOrDefault(e.getId(), List.of()).stream()
                    .map(lender::findBookById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            rec.setCurrentBooks(currentBooks);
            rec.setPreviousBooks(List.of());

            Map<String, Object> recMap = new HashMap<>();
            recMap.put("employee", e);
            recMap.put("record", rec);

            map.put(e.getId(), recMap);
        }

        return map;
    }



}
