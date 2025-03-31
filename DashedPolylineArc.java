package org.example.graphic_editor;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * DashedDraggablePolylineArc відображає ламану пунктирну лінію між двома вузлами (sourceNode, targetNode)
 * з можливістю перетягування waypoint‑точок.
 */
public class DashedPolylineArc extends Group {
    private DraggableNode sourceNode;
    private DraggableNode targetNode;
    private List<Point2D> waypoints;

    private Polyline polyline;
    private Polygon arrowHead;
    private List<Circle> waypointCircles;

    public DashedPolylineArc(DraggableNode source,
                                      DraggableNode target,
                                      List<Point2D> intermediatePoints) {
        this.sourceNode = source;
        this.targetNode = target;
        // Копіюємо список проміжних точок
        this.waypoints = new ArrayList<>(intermediatePoints);

        // Створюємо полілайн із пунктирною лінією
        polyline = new Polyline();
        polyline.setStroke(Color.BLACK);
        polyline.setStrokeWidth(2.0);
        polyline.getStrokeDashArray().addAll(10.0, 10.0);

        // Створюємо стрілку
        arrowHead = new Polygon(
                0.0, 0.0,
                -10.0, 5.0,
                -10.0, -5.0
        );
        arrowHead.setFill(Color.BLACK);

        // Створюємо draggable кола для кожної waypoint‑точки
        waypointCircles = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            Circle c = createWaypointCircle(i);
            waypointCircles.add(c);
        }

        // Додаємо полілайн, стрілку та waypoint‑кола до групи
        getChildren().addAll(polyline, arrowHead);
        getChildren().addAll(waypointCircles);

        // Початковий розрахунок полілайна і розташування стрілки
        recalcPolyline();

        // Додаємо лістенери на зміни позицій sourceNode та targetNode
        sourceNode.layoutXProperty().addListener((obs, oldVal, newVal) -> recalcPolyline());
        sourceNode.layoutYProperty().addListener((obs, oldVal, newVal) -> recalcPolyline());
        targetNode.layoutXProperty().addListener((obs, oldVal, newVal) -> recalcPolyline());
        targetNode.layoutYProperty().addListener((obs, oldVal, newVal) -> recalcPolyline());
    }

    /**
     * Створює draggable коло для waypoint[i].
     */
    private Circle createWaypointCircle(int index) {
        Circle circle = new Circle(5, Color.RED);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(1.0);
        circle.setCursor(Cursor.HAND);

        // Встановлюємо початкову позицію кола
        Point2D wp = waypoints.get(index);
        circle.setCenterX(wp.getX());
        circle.setCenterY(wp.getY());

        // Обробник перетягування: оновлюємо позицію waypoint і перераховуємо полілайн
        circle.setOnMouseDragged(evt -> {
            double newX = evt.getX();
            double newY = evt.getY();
            circle.setCenterX(newX);
            circle.setCenterY(newY);
            waypoints.set(index, new Point2D(newX, newY));
            recalcPolyline();
        });
        return circle;
    }

    /**
     * Перераховуємо координати полілайна: початкова точка (центр source), waypoint‑точки та кінцева точка (центр target).
     */
    private void recalcPolyline() {
        polyline.getPoints().clear();

        double sx = sourceNode.getLayoutX() + sourceNode.getWidth() / 2.0;
        double sy = sourceNode.getLayoutY() + sourceNode.getHeight() / 2.0;
        double tx = targetNode.getLayoutX() + targetNode.getWidth() / 2.0;
        double ty = targetNode.getLayoutY() + targetNode.getHeight() / 2.0;

        List<Double> coords = new ArrayList<>();
        coords.add(sx);
        coords.add(sy);
        for (Point2D wp : waypoints) {
            coords.add(wp.getX());
            coords.add(wp.getY());
        }
        coords.add(tx);
        coords.add(ty);

        polyline.getPoints().addAll(coords);

        updateArrowPosition();
    }

    /**
     * Оновлює позицію стрілки, враховуючи тип targetNode (наприклад, для SERVICE_DEVICE враховуємо центр кола).
     */
    private void updateArrowPosition() {
        int n = polyline.getPoints().size();
        if (n < 4) return; // замало точок

        double x2 = polyline.getPoints().get(n - 2);
        double y2 = polyline.getPoints().get(n - 1);
        double x1 = polyline.getPoints().get(n - 4);
        double y1 = polyline.getPoints().get(n - 3);

        // Якщо targetNode – SERVICE_DEVICE, використовуємо глобальний центр кола
        if (targetNode.getNodeType() == NodeType.SERVICE_DEVICE && targetNode.getDeviceCircle() != null) {
            double localCx = targetNode.getDeviceCircle().getCenterX();
            double localCy = targetNode.getDeviceCircle().getCenterY();
            double globalCx = targetNode.getLayoutX() + localCx;
            double globalCy = targetNode.getLayoutY() + localCy;
            x2 = globalCx;
            y2 = globalCy;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double length = Math.sqrt(dx * dx + dy * dy);

        // Визначення відступу для стрілки залежно від типу targetNode
        double offset;
        switch (targetNode.getNodeType()) {
            case SERVICE_DEVICE:
                double r = targetNode.getRadiusIfServiceDevice();
                offset = (r > 0) ? r : 30.0;
                break;
            case ROUTE_SPLIT:
                offset = 5.0;
                break;
            case QUEUE:
                offset = 35.0;
                break;
            default:
                offset = 30.0;
        }
        double factor = offset / length;
        double xArrow = x2 - dx * factor;
        double yArrow = y2 - dy * factor;

        arrowHead.setTranslateX(xArrow);
        arrowHead.setTranslateY(yArrow);
        arrowHead.setRotate(angle);
    }

    // Геттери (за потреби)
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