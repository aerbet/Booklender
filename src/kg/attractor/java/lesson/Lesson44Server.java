package kg.attractor.java.lesson44;

import com.sun.net.httpserver.HttpExchange;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import kg.attractor.java.model.Book;
import kg.attractor.java.model.Booklender;
import kg.attractor.java.model.Employee;
import kg.attractor.java.model.EmployeeRecords;
import kg.attractor.java.server.BasicServer;
import kg.attractor.java.server.ContentType;
import kg.attractor.java.server.ResponseCodes;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Lesson44Server extends BasicServer {
    private final static Configuration freemarker = initFreeMarker();

    public Lesson44Server(String host, int port) throws IOException {
        super(host, port);
        registerGet("/sample", this::freemarkerSampleHandler);
        registerGet("/register", this::freemarkerRegisterHandler);
        registerGet("/books", this::freemarkerBooksHandler);
        registerGet("/book", this::freemarkerBookHandler);
        registerGet("/employees", this::freemarkerEmployeesHandler);
        registerGet("/employee", this::freemarkerEmployeeHandler);
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
            for (Map.Entry<Employee, EmployeeRecords> entry : lender.getRecords().entrySet()) {
                if (entry.getKey().getId() == employeeId) {
                    found = entry.getKey();
                    empRecords = entry.getValue();
                    break;
                }
            }

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
        renderTemplate(exchange, "books.html", getBooklender());
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

    private void freemarkerRegisterHandler(HttpExchange exchange) {
        renderTemplate(exchange, "register.html", getSampleDataModel());
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

    private SampleDataModel getSampleDataModel() {
        // возвращаем экземпляр тестовой модели-данных
        // которую freemarker будет использовать для наполнения шаблона
        return new SampleDataModel();
    }

    private Booklender getBooklender() {
        return new Booklender();
    }
}
