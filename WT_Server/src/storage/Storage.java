package storage;

import model.Event;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class Storage {

    private static final String FILE_NAME = "events.json";

    // 保存
    public static void save(Map<LocalDate, ArrayList<Event>> eventsInDate) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {

            for (LocalDate date : eventsInDate.keySet()) {
                for (Event e : eventsInDate.get(date)) {
                    // 每个事件一行
                    writer.write(
                            date.toString() + "," +
                                    e.getTime().toString() + "," +
                                    e.getTitle()
                    );
                    writer.newLine();
                }
            }

            System.out.println("[storage.Storage] Saved events to " + FILE_NAME);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载
    public static Map<LocalDate, ArrayList<Event>> load() {
        Map<LocalDate, ArrayList<Event>> map = new HashMap<>();

        File file = new File(FILE_NAME);
        if (!file.exists()) {
            System.out.println("[storage.Storage] No existing file, starting fresh.");
            return map;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // 格式：2026-04-20,18:00,Practice
                String[] parts = line.split(",");
                LocalDate date = LocalDate.parse(parts[0]);
                LocalTime time = LocalTime.parse(parts[1]);
                String title = parts[2];

                Event e = new Event(date, time, title);

                map.computeIfAbsent(date, k -> new ArrayList<>()).add(e);
            }

            System.out.println("[storage.Storage] Loaded events from " + FILE_NAME);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }
}

