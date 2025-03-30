package org.example.graphic_editor;

import java.util.ArrayList;
import java.util.List;

public class SimulationEngine {

    private List<DraggableNode> nodes;
    private List<ArcLine> arcs;
    private EventCalendar calendar;
    private double currentTime;
    private double maxTime;      // Потім зроблю шоб користувач міг задати
    private boolean initialized;

    public SimulationEngine() {
        nodes = new ArrayList<>();
        arcs = new ArrayList<>();
        calendar = new EventCalendar();
        currentTime = 0.0;
        maxTime = 50.0;  // тимчасово
        initialized = false;
    }

    public void setMaxTime(double t) {
        this.maxTime = t;
    }

    public void addNode(DraggableNode node) {
        nodes.add(node);
    }

    public void addArc(ArcLine arc) {
        arcs.add(arc);
    }


    // Ініціалізація перед запуском
    public void initSimulation() {
        currentTime = 0.0;
        calendar = new EventCalendar();

        // Скидаємо стан усіх вузлів
        for (DraggableNode n : nodes) {
            n.initSimulationState();
        }

        // Для генераторів плануємо першу подію DEPARTURE
        for (DraggableNode n : nodes) {
            if (n.getNodeType() == NodeType.GENERATOR) {
                n.scheduleFirstDeparture(calendar, 0.0);
            }
        }

        initialized = true;
        System.out.println("[SimulationEngine] init done.");
    }


    //Повний запуск - доки є події і час < maxTime

    public void runSimulation() {
        if (!initialized) {
            System.out.println("Error: initSimulation() спершу.");
            return;
        }
        while (!calendar.isEmpty()) {
            Event e = calendar.getNextEvent();
            if (e==null) break;
            double evtTime = e.getEventTime();
            if (evtTime>maxTime) break;
            currentTime = evtTime;
            handleEvent(e);
        }
        System.out.println("[SimulationEngine] finished at t=" + currentTime);
    }


    //Одна подія - для кнопки "Simulate Step"
    public void runOneEvent() {
        if (!initialized) {
            System.out.println("Error: викличте initSimulation() спершу.");
            return;
        }
        if (calendar.isEmpty()) {
            System.out.println("Немає більше подій.");
            return;
        }
        Event e = calendar.getNextEvent();
        if (e==null) return;
        double evtTime = e.getEventTime();
        if (evtTime<=maxTime) {
            currentTime=evtTime;
            handleEvent(e);
        } else {
            System.out.println("Подія за межами maxTime.");
        }
    }

    private void handleEvent(Event e) {
        DraggableNode node = e.getNode();
        Request req = e.getRequest();

        switch (e.getEventType()) {
            case ARRIVAL:
                node.handleArrival(currentTime, calendar, req);
                break;
            case DEPARTURE:
                node.handleDeparture(currentTime, calendar, req);
                break;
        }
    }
}
