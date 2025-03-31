package org.example.graphic_editor;

import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Оновлений клас DraggableNode, який успадковується від Pane
 * і відображає різні типи вузлів (GENERATOR, QUEUE, SERVICE_DEVICE, ROUTE_SPLIT).
 */
public class DraggableNode extends Pane {

    private double mouseXOffset;
    private double mouseYOffset;

    // Якщо вузол не SERVICE_DEVICE, можна використати просто Text
    private Text label;

    // Якщо це SERVICE_DEVICE:
    private Circle deviceCircle;   // Коло
    private Text deviceLabel;      // "K"
    private Text statsText;        // Додаткова статистика над колом

    private NodeType nodeType;
    private Double timeDelayMin;
    private Double timeDelayMax;
    private Integer queueMax;

    // -------------------- Для генератора --------------------
    private long generatedCount = 0;
    private Text generatorText;

    // -------------------- Для черги --------------------
    private LinkedList<Request> queueRequests;
    private long queuedCount = 0;
    private Text queueTopText;
    private Text queueBottomText;

    // -------------------- Для пристрою --------------------
    private static int deviceCounter = 0;
    private int deviceId = 0;
    private boolean busy = false;
    private Request currentRequest = null;
    private DraggableNode inputQueueNode = null;
    private double lastStartServiceTime = 0;
    private long countProcessed = 0;
    private double sumServiceTime = 0.0;

    // -------------------- Для розгалуження --------------------
    private long routedCount = 0;
    private Double routeP1, routeP2, routeP3;

    // -------------------- Список вихідних дуг --------------------
    private List<ArcLine> outgoingArcs = new ArrayList<>();

    public DraggableNode(String typeString) {
        super();
        this.setPrefSize(120, 100);

        nodeType = parseNodeType(typeString);
        initDefaultParams();

        // Залежно від типу, малюємо по-різному
        switch (nodeType) {

            case SERVICE_DEVICE:
                deviceCounter++;
                deviceId = deviceCounter;

                // Створюємо коло радіусом 30
                deviceCircle = new Circle(30);
                deviceCircle.setStroke(Color.BLACK);
                deviceCircle.setFill(Color.WHITE);

                // Розташовуємо коло рівно по центру ноди (Pane)
                double centerX = getPrefWidth() / 2.0;
                double centerY = getPrefHeight() / 2.0;
                deviceCircle.setCenterX(centerX);
                deviceCircle.setCenterY(centerY);

                // Напис "K1" (чи "K2" і т.ін.)
                deviceLabel = new Text("K" + deviceId);
                // Припустимо, трохи зсунемо напис, щоб він був приблизно в середині кола
                deviceLabel.setX(centerX - 8);
                deviceLabel.setY(centerY + 5);

                // Текст для статистики
                statsText = new Text();
                // Якщо треба, розміщуємо десь угорі
                statsText.setX(15);
                statsText.setY(-18);

                getChildren().addAll(deviceCircle, deviceLabel, statsText);
                break;

            case GENERATOR:
                generatorText = new Text();
                generatorText.setX(0);
                generatorText.setY(60);
                getChildren().add(generatorText);
                break;

            case QUEUE:
                try {
                    Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("queue.png")));
                    ImageView queueImageView = new ImageView(img);
                    queueImageView.setFitWidth(70);
                    queueImageView.setFitHeight(80);
                    queueImageView.setX(getPrefWidth() / 2.0 - 35);
                    queueImageView.setY(getPrefHeight() / 2.0 - 30);

                    queueTopText = new Text();
                    queueTopText.setX(28);
                    queueTopText.setY(0);

                    queueBottomText = new Text();
                    queueBottomText.setX(getPrefWidth() / 2.0 - 15);
                    queueBottomText.setY(queueImageView.getY() + queueImageView.getFitHeight() + 15);

                    getChildren().addAll(queueImageView, queueTopText, queueBottomText);

                } catch (Exception ex) {
                    System.out.println("Помилка завантаження queue.png: " + ex.getMessage());
                    // fallback
                }
                break;

            case ROUTE_SPLIT:
                // Створимо невелике коло (5 px радіуса), залите чорним
                Circle routeCircle = new Circle(5, Color.BLACK);
                // Обведення чорним (можна опустити, якщо вже fill чорний)
                routeCircle.setStroke(Color.BLACK);
                routeCircle.setStrokeWidth(1.0);

                // Розташуємо коло по центру Pane
                double rcx = getPrefWidth() / 2.0;
                double rcy = getPrefHeight() / 2.0;
                routeCircle.setCenterX(rcx);
                routeCircle.setCenterY(rcy);

                // Текст, де виводитимемо P1, P2, P3
                // Розмістимо його трошки правіше / вище за коло
                label = new Text();
                label.setX(rcx + 10);
                label.setY(rcy - 20);

                // Додаємо це до Pane
                getChildren().addAll(routeCircle, label);
                break;
            default:
                label = new Text(nodeType.name());
                label.setX(10);
                label.setY(25);
                getChildren().add(label);
                break;
        }

        initDragHandlers();
        updateLabel();
    }

    private NodeType parseNodeType(String s) {
        if (s == null) return NodeType.QUEUE;
        String ss = s.trim().toUpperCase();
        switch (ss) {
            case "ГЕНЕРАТОР": return NodeType.GENERATOR;
            case "ЧЕРГА": return NodeType.QUEUE;
            case "ПРИСТРІЙ ОБСЛУГОВУВАННЯ":
            case "ПРИСТРІЙ": return NodeType.SERVICE_DEVICE;
            case "РОЗГАЛУЖЕННЯ": return NodeType.ROUTE_SPLIT;
            default: return NodeType.QUEUE;
        }
    }

    private void initDefaultParams() {
        switch (nodeType) {
            case GENERATOR:
                timeDelayMin = 1.0;
                timeDelayMax = 3.0;
                break;
            case QUEUE:
                queueMax = -1;
                break;
            case SERVICE_DEVICE:
                timeDelayMin = 2.0;
                timeDelayMax = 2.0;
                break;
            case ROUTE_SPLIT:
                routeP1 = 0.5;
                routeP2 = 0.5;
                routeP3 = 0.0;
                break;
        }
    }

    private void updateLabel() {
        switch (nodeType) {
            case SERVICE_DEVICE:
                if (statsText != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Зайнятий: ").append(busy ? "ТАК" : "Ні").append("\n");
                    sb.append("Оброблено: ").append(countProcessed).append("\n");
                    if (countProcessed > 0) {
                        double avg = sumServiceTime / countProcessed;
                        sb.append("Сер.час: ").append(String.format("%.2f", avg));
                    }
                    statsText.setText(sb.toString());
                }
                break;

            case GENERATOR:
                if (generatorText != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("(min)=").append(timeDelayMin).append("\n");
                    sb.append("(max)=").append(timeDelayMax).append("\n");
                    sb.append("Згенеровано=").append(generatedCount);
                    generatorText.setText(sb.toString());
                }
                break;

            case QUEUE:
                if (queueTopText != null) {
                    int currentSize = (queueRequests == null) ? 0 : queueRequests.size();
                    String topStr = "В черзі: " + currentSize + "\nПрийшло: " + queuedCount;
                    queueTopText.setText(topStr);
                }
                if (queueBottomText != null) {
                    String bottomStr = "L=" + ((queueMax == null || queueMax < 0) ? "∞" : queueMax);
                    queueBottomText.setText(bottomStr);
                }
                break;

            case ROUTE_SPLIT:
                if (label != null) {
                    // Формуємо рядок
                    StringBuilder sb = new StringBuilder();
                    sb.append("P1=").append(routeP1).append("; ");
                    sb.append("P2=").append(routeP2);
                    // Якщо користувач вказав P3 (і воно не 0)
                    if (routeP3 != null && routeP3 > 0.0) {
                        sb.append("; P3=").append(routeP3);
                    }
                    label.setText(sb.toString());
                }
                break;

            default:
                if (label != null) {
                    label.setText(nodeType.name());
                }
                break;
        }
    }

    private void initDragHandlers() {
        this.setOnMousePressed((MouseEvent e) -> {
            mouseXOffset = e.getSceneX() - this.getLayoutX();
            mouseYOffset = e.getSceneY() - this.getLayoutY();
            setCursor(Cursor.MOVE);
        });
        this.setOnMouseDragged((MouseEvent e) -> {
            this.setLayoutX(e.getSceneX() - mouseXOffset);
            this.setLayoutY(e.getSceneY() - mouseYOffset);
        });
        this.setOnMouseReleased((MouseEvent e) -> {
            setCursor(Cursor.HAND);
        });
    }

    /**
     * Метод, що повертає глобальний X-координат центру кола (якщо SERVICE_DEVICE),
     * або «умовний центр» вузла для інших типів.
     */
    public double getGlobalCenterX() {
        if (nodeType == NodeType.SERVICE_DEVICE && deviceCircle != null) {
            return getLayoutX() + deviceCircle.getCenterX();
        } else {
            return getLayoutX() + getWidth()/2.0;
        }
    }

    /**
     * Аналогічно, глобальний Y-координат центру кола / вузла.
     */
    public double getGlobalCenterY() {
        if (nodeType == NodeType.SERVICE_DEVICE && deviceCircle != null) {
            return getLayoutY() + deviceCircle.getCenterY();
        } else {
            return getLayoutY() + getHeight()/2.0;
        }
    }

    // Якщо треба напряму коло
    public Circle getDeviceCircle() {
        if (nodeType == NodeType.SERVICE_DEVICE) {
            return deviceCircle;
        }
        return null;
    }

    // Повертаємо радіус, якщо це пристрій
    public double getRadiusIfServiceDevice() {
        if (nodeType == NodeType.SERVICE_DEVICE && deviceCircle != null) {
            return deviceCircle.getRadius();
        }
        return -1;
    }

    public void applyDialogResult(NodeParameterDialog.ResultData r) {
        switch (nodeType) {
            case GENERATOR:
            case SERVICE_DEVICE:
                timeDelayMin = r.getTimeDelayMin();
                timeDelayMax = r.getTimeDelayMax();
                break;
            case QUEUE:
                queueMax = r.getQueueMax();
                break;
            case ROUTE_SPLIT:
                routeP1 = (r.getP1() == null) ? 0.0 : r.getP1();
                routeP2 = (r.getP2() == null) ? 0.0 : r.getP2();
                routeP3 = (r.getP3() == null) ? 0.0 : r.getP3();
                assignProbabilitiesToArcs();
                System.out.println("[ROUTE_SPLIT] user set p1=" + routeP1
                        + ", p2=" + routeP2
                        + ", p3=" + routeP3);
                break;
        }
        updateLabel();
    }

    private void assignProbabilitiesToArcs() {
        if (nodeType != NodeType.ROUTE_SPLIT) return;
        if (outgoingArcs.isEmpty()) return;

        if (outgoingArcs.size() == 1) {
            outgoingArcs.get(0).setProbability(1.0);
        } else if (outgoingArcs.size() == 2) {
            outgoingArcs.get(0).setProbability(routeP1);
            outgoingArcs.get(1).setProbability(routeP2);
        } else {
            outgoingArcs.get(0).setProbability(routeP1);
            outgoingArcs.get(1).setProbability(routeP2);
            outgoingArcs.get(2).setProbability(routeP3);
            for (int i = 3; i < outgoingArcs.size(); i++) {
                outgoingArcs.get(i).setProbability(0.0);
            }
        }
    }

    public void initSimulationState() {
        generatedCount = 0;
        routedCount = 0;
        queuedCount = 0;

        busy = false;
        currentRequest = null;
        countProcessed = 0;
        sumServiceTime = 0.0;
        lastStartServiceTime = 0;

        if (nodeType == NodeType.QUEUE) {
            queueRequests = new LinkedList<>();
        } else {
            queueRequests = null;
        }
        updateLabel();
    }

    public void setInputQueueNode(DraggableNode q) {
        inputQueueNode = q;
    }
    public DraggableNode getInputQueueNode() {
        return inputQueueNode;
    }

    public void addOutgoingArc(ArcLine arc) {
        outgoingArcs.add(arc);
        if (nodeType == NodeType.ROUTE_SPLIT) {
            assignProbabilitiesToArcs();
        }
    }
    public List<ArcLine> getOutgoingArcs() {
        return outgoingArcs;
    }

    // ---------------- handleArrival / handleDeparture ----------------

    public void handleArrival(double currentTime, EventCalendar calendar, Request request) {
        switch (nodeType) {
            case QUEUE:
                handleArrivalQueue(currentTime, calendar, request);
                break;
            case SERVICE_DEVICE:
                if (inputQueueNode == null) {
                    // direct ARRIVAL
                    handleArrivalDeviceDirect(currentTime, calendar, request);
                } else {
                    System.out.println("[DEVICE-pull] відмова прямого ARRIVAL");
                }
                break;
            case ROUTE_SPLIT:
                routedCount++;
                routeToNextNode(currentTime, calendar, request);
                break;
            case GENERATOR:
                // генератор не приймає ARRIVAL
                break;
        }
        updateLabel();
    }

    public void handleDeparture(double currentTime, EventCalendar calendar, Request request) {
        switch (nodeType) {
            case GENERATOR:
                generatedCount++;
                routeToNextNode(currentTime, calendar, request);
                double dd = randUniform(timeDelayMin, timeDelayMax);
                double nextT = currentTime + dd;
                Request r2 = new Request();
                Event ev = new Event(nextT, EventType.DEPARTURE, this, r2);
                calendar.addEvent(ev);
                break;

            case SERVICE_DEVICE:
                busy = false;
                currentRequest = null;
                countProcessed++;
                double stime = currentTime - lastStartServiceTime;
                sumServiceTime += stime;
                System.out.println("[DEVICE] Завершили " + request.getId() + " за " + stime);

                routeToNextNode(currentTime, calendar, request);
                pullFromQueueIfExists(calendar, currentTime);
                break;

            case QUEUE:
                break;
            case ROUTE_SPLIT:
                break;
        }
        updateLabel();
    }

    // ---- Логіка черги: надходження ARRIVAL ----
    private void handleArrivalQueue(double currentTime, EventCalendar calendar, Request req) {
        queuedCount++;
        if (queueRequests == null) {
            queueRequests = new LinkedList<>();
        }
        int cap = (queueMax == null) ? -1 : queueMax;
        if (cap < 0 || queueRequests.size() < cap) {
            queueRequests.add(req);
            System.out.println("[QUEUE] Запит " + req.getId() + " => черга (size=" + queueRequests.size() + ")");
            tryPushOrPullFromQueue(calendar, currentTime);
        } else {
            System.out.println("[QUEUE] Переповнена => відмова " + req.getId());
        }
    }

    /**
     * Якщо target = SERVICE_DEVICE і він pull -> викликаємо pullFromQueueIfExists
     * Якщо target = ROUTE_SPLIT (або інше) -> виконуємо push
     */
    private void tryPushOrPullFromQueue(EventCalendar calendar, double currentTime) {
        for (ArcLine arc : outgoingArcs) {
            DraggableNode nextNode = arc.getTargetNode();
            switch (nextNode.getNodeType()) {
                case SERVICE_DEVICE:
                    if (nextNode.getInputQueueNode() == this) {
                        if (!nextNode.busy) {
                            nextNode.pullFromQueueIfExists(calendar, currentTime);
                        }
                    } else {
                        pushAllFromQueueToNode(calendar, currentTime, nextNode);
                    }
                    break;
                case ROUTE_SPLIT:
                case GENERATOR:
                case QUEUE:
                    pushAllFromQueueToNode(calendar, currentTime, nextNode);
                    break;
            }
        }
    }

    private void pushAllFromQueueToNode(EventCalendar calendar, double currentTime, DraggableNode nextNode) {
        if (queueRequests == null) return;
        while (!queueRequests.isEmpty()) {
            Request front = queueRequests.removeFirst();
            System.out.println("[QUEUE->NEXT] push req " + front.getId() + " -> " + nextNode.getNodeType());
            calendar.addEvent(new Event(currentTime, EventType.ARRIVAL, nextNode, front));
        }
    }

    // direct ARRIVAL для пристрою (без pull)
    private void handleArrivalDeviceDirect(double currentTime, EventCalendar calendar, Request req) {
        if (!busy) {
            busy = true;
            currentRequest = req;
            lastStartServiceTime = currentTime;
            double servTime = randUniform(timeDelayMin, timeDelayMax);
            double finishT = currentTime + servTime;
            System.out.println("[DEVICE-direct] Почали " + req.getId() + " до t=" + finishT);
            calendar.addEvent(new Event(finishT, EventType.DEPARTURE, this, req));
        } else {
            System.out.println("[DEVICE-direct] Зайнятий => відмова " + req.getId());
        }
    }

    // Пристрій "витягує" запити з черги, якщо він вільний
    private void pullFromQueueIfExists(EventCalendar calendar, double currentTime) {
        if (nodeType != NodeType.SERVICE_DEVICE) return;
        if (busy) return;
        if (inputQueueNode != null && inputQueueNode.nodeType == NodeType.QUEUE) {
            LinkedList<Request> q = inputQueueNode.queueRequests;
            if (q != null && !q.isEmpty()) {
                Request next = q.removeFirst();
                busy = true;
                currentRequest = next;
                lastStartServiceTime = currentTime;
                double st = randUniform(timeDelayMin, timeDelayMax);
                double fin = currentTime + st;
                Event e = new Event(fin, EventType.DEPARTURE, this, next);
                calendar.addEvent(e);
                System.out.println("[DEVICE-pull] Витягнули " + next.getId() + " до t=" + fin);
            }
        }
    }

    // routeToNextNode (GENERATOR, ROUTE_SPLIT, DEVICE)
    private void routeToNextNode(double currentTime, EventCalendar calendar, Request request) {
        if (outgoingArcs.isEmpty()) {
            System.out.println("[EXIT] Запит " + request.getId() + " виходить із системи");
            return;
        }
        if (outgoingArcs.size() == 1) {
            ArcLine arc = outgoingArcs.get(0);
            calendar.addEvent(new Event(currentTime, EventType.ARRIVAL, arc.getTargetNode(), request));
            return;
        }
        double sum = 0.0;
        for (ArcLine arc : outgoingArcs) sum += arc.getProbability();
        if (sum <= 0.0) {
            System.out.println("[ROUTE_SPLIT] Імовірності=0 => fallback => остання");
            ArcLine last = outgoingArcs.get(outgoingArcs.size() - 1);
            calendar.addEvent(new Event(currentTime, EventType.ARRIVAL, last.getTargetNode(), request));
            return;
        }
        double r = Math.random() * sum;
        double cum = 0.0;
        for (ArcLine arc : outgoingArcs) {
            cum += arc.getProbability();
            if (r <= cum) {
                calendar.addEvent(new Event(currentTime, EventType.ARRIVAL, arc.getTargetNode(), request));
                return;
            }
        }
        // якщо нічого не підійшло, fallback
        ArcLine last = outgoingArcs.get(outgoingArcs.size() - 1);
        calendar.addEvent(new Event(currentTime, EventType.ARRIVAL, last.getTargetNode(), request));
    }

    // Якщо це GENERATOR, плануємо першу DEPARTURE
    public void scheduleFirstDeparture(EventCalendar calendar, double startTime) {
        if (nodeType == NodeType.GENERATOR) {
            double dd = randUniform(timeDelayMin, timeDelayMax);
            double dt = startTime + dd;
            Request req = new Request();
            Event ev = new Event(dt, EventType.DEPARTURE, this, req);
            calendar.addEvent(ev);
            System.out.println("[GENERATOR] Перший DEPARTURE @t=" + dt);
        }
    }

    private double randUniform(Double a, Double b) {
        if (a == null || b == null || a >= b) return 1.0;
        return a + (b - a) * Math.random();
    }

    // ---------------- Getter’и для параметрів ----------------

    public NodeType getNodeType() {
        return nodeType;
    }
    public Double getTimeDelayMin() {
        return timeDelayMin;
    }
    public Double getTimeDelayMax() {
        return timeDelayMax;
    }
    public Integer getQueueMax() {
        return queueMax;
    }
}