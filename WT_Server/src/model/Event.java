package model;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

public class Event {
    private LocalDate date;
    private LocalTime time;
    private String title;  //topic of the sermon
    private ArrayList<Song> songs = new ArrayList<>();
    private ArrayList<Member> team = new ArrayList<>(); //members who signed up to serve this event

    public Event(LocalDate date, LocalTime time, String title) {
        this.date = date;
        this.time = time;
        this.title = title;
    }

    //getter and setter

    public LocalDate getDate() {return date;}
    public void setDate(LocalDate date) {this.date = date;}

    public LocalTime getTime() {return time;}
    public void setTime(LocalTime time) {this.time = time;}

    public String getTitle() {return title;}
    public void setTitle(String title) {this.title = title;}

    public ArrayList<Song> getSongs() {return songs;} //notice: returned an ArrayList

    public void addSong(Song song) {
        if (songs.contains(song)) {return;} //if the song already exist in the list, don't do anything
        songs.add(song); //add the song to songs
    }

    public void removeSong(Song song) {
        if (!songs.contains(song)) {return;} //if the song doesn't exist, don't do anything
        songs.remove(song); //remove the song to songs
    }

    public ArrayList<Member> getTeam() {return team;}

    public void addtoTeam(Member member) {
        if (team.contains(member)) {return;} //if the member already exist in the team, don't do anything
        team.add(member); // add the member to team
    }

    public void removefromTeam(Member member) {
        if (!team.contains(member)) {return;} //if the member doesn't exist, don't do anything
        team.remove(member); //remove the member form team
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Date:").append(getDate()).append("|");
        sb.append("Time:").append(getTime()).append("|");
        sb.append("Title:").append(getTitle()).append("|");

        sb.append("Songs:" + "|");
        for (Song song : getSongs()) {
            sb.append("\t").append(song).append("|");
        }

        sb.append("Team:" + "|");
        for (Member member : getTeam()) {
            sb.append("\t").append(member).append("|");
        }

        return sb.toString();
    }
}
