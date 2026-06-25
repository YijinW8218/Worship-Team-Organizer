package storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserActivity {

    private final List<String> activities = new ArrayList<>();
    private final File file = new File("userActivitiesLog.csv");

    public UserActivity() {
        load();
    }

    public void logUserActivity(String currentUser, String msgFromClient) {
        String username = currentUser == null ? "Unknown" : currentUser;
        activities.add(LocalDateTime.now() + "," + username + "," + msgFromClient);
        save();
    }

    public List<String> getActivities() {
        return new ArrayList<>(activities);
    }

    private void load() {
        try {
            if (!file.exists()) {
                System.out.println("[storage.UserActivity] No existing file, starting fresh.");
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    activities.add(line);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (String activity : activities) {
                writer.write(activity);
                writer.newLine();
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
