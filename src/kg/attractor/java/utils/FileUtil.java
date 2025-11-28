package kg.attractor.java.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kg.attractor.java.model.BooklenderData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    private FileUtil() {}

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);


    private static final Path DATA_PATH = Paths.get("src", "kg", "attractor", "java", "bookData", "data.json");

    public static BooklenderData readData() {
        try {
            if (!Files.exists(DATA_PATH)) {
                if (DATA_PATH.getParent() != null) {
                    Files.createDirectories(DATA_PATH.getParent());
                }
                BooklenderData empty = new BooklenderData();
                saveData(empty);
                return empty;
            }
            return MAPPER.readValue(DATA_PATH.toFile(), BooklenderData.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new BooklenderData();
        }
    }

    public static void saveData(BooklenderData data) {
        try {
            if (DATA_PATH.getParent() != null) {
                Files.createDirectories(DATA_PATH.getParent());
            }
            MAPPER.writeValue(DATA_PATH.toFile(), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
