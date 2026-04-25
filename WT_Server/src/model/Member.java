package model;

import java.time.LocalDateTime;

public class Member {
    private final int id;
    private String userName; //The name that shows to other users
    private String password;
    private final LocalDateTime createdAt;

    public Member(int id, String userName, String password) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }


    //getter and setter

    public int getId() {return id;}

    public String getUserName() {return userName;}
    public void setUserName(String userName) {this.userName = userName;}

    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}

    public LocalDateTime getCreatedAt() {return createdAt;}

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
