package model;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Member {
    private final int id;
    private String userName; //The name that shows to other users
    private String password;
    private ArrayList<Event> schedule  = new ArrayList<>();;

    public Member(int id, String userName, String password) {
        this.id = id;
        this.userName = userName;
        this.password = password;
    }


    //getter and setter

    public int getId() {return id;}

    public String getUserName() {return userName;}

    public String getPassword() {return password;}

    public void addEvent(Event e) {if (!schedule.contains(e)) {schedule.add(e);}}
    public void removeEvent(Event e) {schedule.remove(e);}

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                '}';
    }
}
