package org.example.graphic_editor;

import java.util.PriorityQueue;

public class EventCalendar {
    private PriorityQueue<Event> eventQueue;

    public EventCalendar() {
        eventQueue = new PriorityQueue<>();
    }

    public void addEvent(Event e) {
        eventQueue.offer(e);
    }

    public Event getNextEvent() {
        return eventQueue.poll();
    }

    public boolean isEmpty() {
        return eventQueue.isEmpty();
    }
}