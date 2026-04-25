import model.Calendar;
import model.Event;

import java.time.LocalDate;
import java.time.LocalTime;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //this is a single end model to run the logic. Server and Client not included.
        Calendar calendar = new Calendar();

        """
        //try: create events instance
        Event e1 = new Event( LocalDate.of(2026, 4, 20), LocalTime.of(18, 0), "Practice");
        Event e2 = new Event( LocalDate.of(2026, 4, 20), LocalTime.of(10, 0), "Sunday Service");

        //try: add event to calendar
        calendar.addEvent(e1);
        calendar.addEvent(e2);

        //try: check get event
        System.out.println("Events on 4/20:");
        System.out.println(calendar.getEvents(LocalDate.of(2026, 4, 20)));

        //try: remove and check
        calendar.removeEvent(e1);
        System.out.println(calendar.getEvents(LocalDate.of(2026, 4, 20)));
        calendar.removeEvent(e2);
        System.out.println(calendar.getEvents(LocalDate.of(2026, 4, 20)));
"""
        Server server = new Server();
        try {
            server.listenClient(1234);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}