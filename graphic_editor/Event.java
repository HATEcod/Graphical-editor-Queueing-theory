package org.example.graphic_editor;

public class Event implements Comparable<Event> {
    private double eventTime;
    private EventType eventType;
    private DraggableNode node;
    private Request request;

    public Event(double eventTime, EventType eventType, DraggableNode node, Request request) {
        this.eventTime = eventTime;
        this.eventType = eventType;
        this.node = node;
        this.request = request;
    }

    public double getEventTime() {
        return eventTime;
    }
    public EventType getEventType() {
        return eventType;
    }
    public DraggableNode getNode() {
        return node;
    }
    public Request getRequest() {
        return request;
    }

    @Override
    public int compareTo(Event other) {
        return Double.compare(this.eventTime, other.eventTime);
    }
}
