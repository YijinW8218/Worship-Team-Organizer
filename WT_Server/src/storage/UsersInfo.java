package storage;

import model.Member;

import java.io.*;
import java.util.ArrayList;

public class UsersInfo {
    private ArrayList<Member> users = new ArrayList<>();
    private final File file = new File("users.csv");

    public UsersInfo() { load(); }

    // check if username exists and password is correct
    public boolean login(String username, String password) {
        for (Member m : users) {
            if (m.getUserName().equals(username) && m.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    // add a new user
    public boolean register(String username, String password) {
        if (getMemberByName(username) != null) {return false;} // repeated username not allowed
        // create new member
        int newId = users.size() + 1;
        Member m = new Member(newId, username, password);
        users.add(m); // add to list
        save(); // save to file
        return true;
    }

    public Member getMemberByName(String name) {
        for (Member m : users) { if (m.getUserName().equals(name)) return m; }
        return null;
    }


    // read from file
    private void load() {
        try {
            if (!file.exists()) {
                System.out.println("[storage.UsersInfo] No existing file, starting fresh.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 3) {
                        users.add(new Member(Integer.parseInt(parts[0]), parts[1], parts[2])); // new member with id, username, password
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // write to file
    public void save() {
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Member m : users) {
                    writer.write(m.getId() + "," + m.getUserName() + "," + m.getPassword());
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}