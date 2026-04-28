package model;

import storage.Storage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// this class manage different events on different dates.
public class Calendar {
    private Map<LocalDate, ArrayList<Event>> eventsInDate = new HashMap<>(); //the list of all the events in one day + the date

    public Calendar() {
        //constructor
        eventsInDate = Storage.load();
    }

    public void addEvent(Event event) {
        LocalDate date = event.getDate();
        eventsInDate.putIfAbsent(date, new ArrayList<>()); //if date is not a key yet, create new key
        eventsInDate.get(date).add(event);
    }

    public ArrayList<Event> getEvents(LocalDate date) {
        if (!eventsInDate.containsKey(date)) {return new ArrayList<>();}//if this date don't exist, stop methods and return
        ArrayList<Event> eventsList = eventsInDate.get(date); //get the list of events in this day
        return eventsList;
    }

    public void removeEvent(Event event) {
        LocalDate date = event.getDate();
        ArrayList<Event> eventsList = getEvents(date);
        eventsList.remove(event); //remove this event from the list

        if (eventsList.isEmpty()) {
            eventsInDate.remove(date); //if the date is empty, delete the key from calendar
        }
    }

    public Event searchByTitle(String title) {
        for (LocalDate date : eventsInDate.keySet()) {
            for (Event event : eventsInDate.get(date)) {
                if (event.getTitle().equalsIgnoreCase(title)) {
                    return event; // return the first Event among the same name
                }
            }
        }
        return null;
    }


    public void save() {
        Storage.save(eventsInDate);
    }





    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<LocalDate, ArrayList<Event>> entry : eventsInDate.entrySet()) {
            sb.append(entry.getKey()).append(": ");

            for (Event e : entry.getValue()) {
                sb.append(e.getTitle()).append(" ");
            }

            sb.append("\n");
        }

        return sb.toString();

    }
}
