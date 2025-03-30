package org.example.graphic_editor;

public class Request {
    private static long globalId = 0;
    private long id;

    public Request() {
        id = ++globalId;
    }

    public long getId() {
        return id;
    }
}
