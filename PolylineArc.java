package org.example.graphic_editor;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * PolylineArc відображає ламану лінію між двома вузлами (sourceNode, targetNode),
 * з урахуванням проміжних точок, які можна перетягувати. Стрілка на кінці
 * "прилипає" до краю кола пристрою, якщо targetNode — SERVICE_DEVICE.
 */
public class PolylineArc extends Group {

    private DraggableNode sourceNode;
    private DraggableNode targetNode;

    /**
     * Список проміжних точок у "глобальних" координатах Pane.
     */
    private List<Point2D> waypoints;

    // Запам'ятовуємо попередні позиції вузлів, щоб зсувати points при русі самих вузлів
    private double oldSourceX;
    private double oldSourceY;
    private double oldTargetX;
    private double oldTargetY;

    // Основні графічні елементи
    private Polyline polyline;
    private Polygon arrowHead;

    // Кола для кожної проміжної точки (щоб можна було їх перетягувати)
    private List<Circle> waypointCircles;

    public PolylineArc(DraggableNode source,
                       DraggableNode target,
                       List<Point2D> intermediatePoints)
    {
        this.sourceNode = source;
        this.targetNode = target;
        this.waypoints  = new ArrayList<>(intermediatePoints);

        // Запам'ятовуємо початкові координати вузлів, щоб при русі знати зсув
        this.oldSourceX = sourceNode.getLayoutX();
        this.oldSourceY = sourceNode.getLayoutY();
        this.oldTargetX = targetNode.getLayoutX();
        this.oldTargetY = targetNode.getLayoutY();

        // Полілайн — головний "шлях" між source і target
        polyline = new Polyline();
        polyline.setStroke(Color.BLACK);
        polyline.setStrokeWidth(2.0);

        // Стрілка (трикутник)
        arrowHead = new Polygon(
                0.0, 0.0,
                -10.0, 5.0,
                -10.0, -5.0
        );
        arrowHead.setFill(Color.BLACK);

        // Створимо кілечка для проміжних точок
        waypointCircles = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            Circle c = createWaypointCircle(i);
            waypointCircles.add(c);
        }

        // Додаємо полілайн, стрілку і кола в групу
        getChildren().addAll(polyline, arrowHead);
        getChildren().addAll(waypointCircles);

        // Перший розрахунок шляху
        recalcPolyline();

        // Лістенери, щоб при зміні координат вузлів зсувати проміжні точки
        sourceNode.layoutXProperty().addListener(this::onSourceMoved);
        sourceNode.layoutYProperty().addListener(this::onSourceMoved);
        targetNode.layoutXProperty().addListener(this::onTargetMoved);
        targetNode.layoutYProperty().addListener(this::onTargetMoved);
    }

    /**
     * Створює коло (radius=5) для waypoint[i], щоб можна було його перетягувати.
     */
    private Circle createWaypointCircle(int index) {
        Circle circle = new Circle(5, Color.RED);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(1.0);
        circle.setCursor(Cursor.HAND);

        // Початкова позиція кола
        Point2D wp = waypoints.get(index);
        circle.setCenterX(wp.getX());
        circle.setCenterY(wp.getY());

        // Обробник перетягування
        circle.setOnMouseDragged(evt -> {
            // Нові координати
            double newX = evt.getX();
            double newY = evt.getY();
            // Переміщаємо коло
            circle.setCenterX(newX);
            circle.setCenterY(newY);
            // Оновлюємо waypoint
            waypoints.set(index, new Point2D(newX, newY));
            // Перерахунок полілайна
            recalcPolyline();
        });

        return circle;
    }

    /**
     * Лістенер для руху sourceNode: зсуваємо всі проміжні точки і їхні кола.
     */
    private void onSourceMoved(ObservableValue<? extends Number> obs,
                               Number oldVal, Number newVal)
    {
        double newX = sourceNode.getLayoutX();
        double newY = sourceNode.getLayoutY();
        double dx = newX - oldSourceX;
        double dy = newY - oldSourceY;

        // Зсуваємо проміжні точки
        for (int i = 0; i < waypoints.size(); i++) {
            Point2D oldPoint = waypoints.get(i);
            Point2D shifted = new Point2D(oldPoint.getX() + dx, oldPoint.getY() + dy);
            waypoints.set(i, shifted);
        }
        // Зсуваємо кола
        for (int i = 0; i < waypointCircles.size(); i++) {
            Circle c = waypointCircles.get(i);
            c.setCenterX(c.getCenterX() + dx);
            c.setCenterY(c.getCenterY() + dy);
        }

        oldSourceX = newX;
        oldSourceY = newY;

        recalcPolyline();
    }

    /**
     * Лістенер для руху targetNode: зсуваємо всі проміжні точки і кола.
     */
    private void onTargetMoved(ObservableValue<? extends Number> obs,
                               Number oldVal, Number newVal)
    {
        double newX = targetNode.getLayoutX();
        double newY = targetNode.getLayoutY();
        double dx = newX - oldTargetX;
        double dy = newY - oldTargetY;

        for (int i = 0; i < waypoints.size(); i++) {
            Point2D oldPoint = waypoints.get(i);
            Point2D shifted = new Point2D(oldPoint.getX() + dx, oldPoint.getY() + dy);
            waypoints.set(i, shifted);
        }
        for (Circle c : waypointCircles) {
            c.setCenterX(c.getCenterX() + dx);
            c.setCenterY(c.getCenterY() + dy);
        }

        oldTargetX = newX;
        oldTargetY = newY;

        recalcPolyline();
    }

    /**
     * Переформовує полілайн (список точок), а також оновлює позицію стрілки.
     * Шлях: (центр sourceNode) + waypoints + (центр targetNode).
     */
    private void recalcPolyline() {
        polyline.getPoints().clear();

        double sx = sourceNode.getLayoutX() + sourceNode.getWidth()/2.0;
        double sy = sourceNode.getLayoutY() + sourceNode.getHeight()/2.0;
        double tx = targetNode.getLayoutX() + targetNode.getWidth()/2.0;
        double ty = targetNode.getLayoutY() + targetNode.getHeight()/2.0;

        List<Double> coords = new ArrayList<>();
        coords.add(sx); coords.add(sy);
        for (Point2D wp : waypoints) {
            coords.add(wp.getX());
            coords.add(wp.getY());
        }
        coords.add(tx); coords.add(ty);

        polyline.getPoints().addAll(coords);

        updateArrowPosition();
    }

    /**
     * Розташовує стрілку біля кінця полілайна, повертає її за напрямком.
     * Якщо targetNode — SERVICE_DEVICE (коло), використовуємо його радіус
     * замість фіксованих 30 пікселів.
     */
    private void updateArrowPosition() {
        int n = polyline.getPoints().size();
        if (n < 4) return; // замало точок

        // Останні дві точки ламаної (x1,y1) -> (x2,y2)
        double x2 = polyline.getPoints().get(n - 2);
        double y2 = polyline.getPoints().get(n - 1);
        double x1 = polyline.getPoints().get(n - 4);
        double y1 = polyline.getPoints().get(n - 3);

        // Якщо targetNode — SERVICE_DEVICE, вирахуємо справжній центр кола
        if (targetNode.getNodeType() == NodeType.SERVICE_DEVICE
                && targetNode.getDeviceCircle() != null) {
            double localCx = targetNode.getDeviceCircle().getCenterX();
            double localCy = targetNode.getDeviceCircle().getCenterY();
            double globalCx = targetNode.getLayoutX() + localCx;
            double globalCy = targetNode.getLayoutY() + localCy;

            // Останній відрізок ламаної: x1,y1 лишаємо, а x2,y2 = глобальний центр кола
            x1 = polyline.getPoints().get(n - 4);
            y1 = polyline.getPoints().get(n - 3);
            x2 = globalCx;
            y2 = globalCy;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double length = Math.sqrt(dx*dx + dy*dy);

        // Визначаємо відступ залежно від типу targetNode
        NodeType ttype = targetNode.getNodeType();
        double offset;
        switch (ttype) {
            case SERVICE_DEVICE:
                // Коло (пристрій)
                double r = targetNode.getRadiusIfServiceDevice();
                offset = (r > 0) ? r : 30.0;  // Можна взяти (r + 2) чи (r - 2)
                break;
            case ROUTE_SPLIT:
                // Розгалуження (маленький кружечок)
                offset = 5.0;
                break;
            case QUEUE:
                // Черга (зображення) – збільшуємо
                offset = 35.0;
                break;
            default:
                // Решта типів
                offset = 30.0;
        }

        double factor = offset / length;
        double xArrow = x2 - dx * factor;
        double yArrow = y2 - dy * factor;

        arrowHead.setTranslateX(xArrow);
        arrowHead.setTranslateY(yArrow);
        arrowHead.setRotate(angle);
    }

    // Геттери
    public DraggableNode getSourceNode() {
        return sourceNode;
    }
    public DraggableNode getTargetNode() {
        return targetNode;
    }
    public List<Point2D> getWaypoints() {
        return waypoints;
    }
}
