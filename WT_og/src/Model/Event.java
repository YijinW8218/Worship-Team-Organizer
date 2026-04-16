package Model;
import java.time.LocalDate;
import java.time.LocalTime;

public class Event {
    private LocalDate date;
    private LocalTime time;
    private String title;  //topic of the sermon

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


    @Override
    public String toString() {
        return "Event{" +
                "date=" + date +
                ", time=" + time +
                ", title='" + title + '\'' +
                '}';
    }
}
