package Model;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

public class Event {
    private LocalDate date;
    private LocalTime time;
    private String title;  //topic of the sermon
    private ArrayList<Song> songs;

    public Event(LocalDate date, LocalTime time, String title, ArrayList<Song> songs) {
        this.date = date;
        this.time = time;
        this.title = title;
        this.songs = songs;
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
        if (songs.contains(song)) {return;} //if the song already exist, don't do anything
        songs.add(song);
    }

    public void removeSong(Song song) {
        if (!songs.contains(song)) {return;} //if the song doesn't exist, don't do anything
        songs.remove(song);
    }

    @Override
    public String toString() {
        return "Event{" +
                "date=" + date +
                ", time=" + time +
                ", title='" + title + '\'' +
                '}';
    }
}
