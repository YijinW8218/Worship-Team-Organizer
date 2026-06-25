package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

public class Member {
    private final int id;
    private String userName; //The name that shows to other users
    private String password;
    private String role = "";
    private ArrayList<Event> schedule  = new ArrayList<>();;

    public Member(int id, String userName, String password) {
        this.id = id;
        this.userName = userName;
        this.password = password;
    }

    public Member(int id, String userName, String password, String role) {
        this(id, userName, password);
        this.role = role == null ? "" : role;
    }


    //getter and setter

    public int getId() {return id;}

    public String getUserName() {return userName;}

    public String getPassword() {return password;}

    public String getRole() {return role;}

    public void setRole(String role) {this.role = role == null ? "" : role;}

    public void addEvent(Event e) {if (!schedule.contains(e)) {schedule.add(e);}}
    public void removeEvent(Event e) {schedule.remove(e);}

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Member)) {
            return false;
        }
        Member other = (Member) obj;
        return userName.equalsIgnoreCase(other.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName.toLowerCase());
    }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                '}';
    }
}
