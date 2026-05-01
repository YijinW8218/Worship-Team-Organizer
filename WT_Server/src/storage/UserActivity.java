package storage;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserActivity {

    private Map<String, String> useracts = new HashMap<>(); //currentUser, action
    private final File file = new File("userActivitiesLog.csv");

    public UserActivity() { load(); }

    public void logUserActivity(String currentUser, String msgFromClient) {
        useracts.put(currentUser, msgFromClient); //add to map
        save(); // save to file
    }

    // read from file
    private void load() {
        try {
            if (!file.exists()) {
                System.out.println("[storage.UserActivity] No existing file, starting fresh.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        useracts.put(parts[0], parts[1]); // currentUser, action
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // write to file
    private void save() {
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Map.Entry<String, String> entry : useracts.entrySet()) {
                    writer.write(entry.getKey() + "," + entry.getValue());
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
