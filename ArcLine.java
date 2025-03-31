package org.example.graphic_editor;

import javafx.beans.binding.Bindings;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Клас ArcLine:
 *  - Якщо відсутні проміжні точки (waypoints), відображається лише mainLine + стрілка.
 *  - Якщо є проміжні точки, додається полілайн (ламана).
 */
public class ArcLine extends Group {

    private DraggableNode sourceNode;
    private DraggableNode targetNode;
    private double probability;

    private Line mainLine;
    private Polyline polyline;
    private List<Point2D> waypoints = new ArrayList<>();
    private Polygon arrowHead;

    public ArcLine(DraggableNode source, DraggableNode target, double probability) {
        this.sourceNode = source;
        this.targetNode = target;
        this.probability = probability;

        // Головна лінія (для прямого з'єднання)
        mainLine = new Line();
        mainLine.setStroke(Color.BLACK);
        mainLine.setStrokeWidth(2.0);

        // Прив’язки до центру source / target
        mainLine.startXProperty().bind(
                Bindings.createDoubleBinding(
                        () -> sourceNode.getGlobalCenterX(),
                        // Слухаємо layoutX, layoutY (і, за потреби, radius, якщо змінний)
                        sourceNode.layoutXProperty(),
                        sourceNode.layoutYProperty()
                )
        );
        mainLine.startYProperty().bind(
                Bindings.createDoubleBinding(
                        () -> sourceNode.getGlobalCenterY(),
                        sourceNode.layoutXProperty(),
                        sourceNode.layoutYProperty()
                )
        );

// Те саме для endX, endY, але з targetNode
        mainLine.endXProperty().bind(
                Bindings.createDoubleBinding(
                        () -> targetNode.getGlobalCenterX(),
                        targetNode.layoutXProperty(),
                        targetNode.layoutYProperty()
                )
        );
        mainLine.endYProperty().bind(
                Bindings.createDoubleBinding(
                        () -> targetNode.getGlobalCenterY(),
                        targetNode.layoutXProperty(),
                        targetNode.layoutYProperty()
                )
        );

        // Полілайн (додаємо тільки, якщо setWaypoints(...) непорожній)
        polyline = new Polyline();
        polyline.setStroke(Color.BLACK);
        polyline.setStrokeWidth(2.0);

        // Стрілка
        arrowHead = new Polygon(
                0.0, 0.0,
                -10.0, 5.0,
                -10.0, -5.0
        );
        arrowHead.setFill(Color.BLACK);

        // Лістенери: при зміні startX/Y endX/Y – оновити стрілку
        mainLine.startXProperty().addListener((obs, oldVal, newVal) -> updateArrow());
        mainLine.startYProperty().addListener((obs, oldVal, newVal) -> updateArrow());
        mainLine.endXProperty().addListener((obs, oldVal, newVal) -> updateArrow());
        mainLine.endYProperty().addListener((obs, oldVal, newVal) -> updateArrow());

        // Додаємо посилання на цю дугу у sourceNode
        sourceNode.addOutgoingArc(this);

        // Спочатку лише mainLine + стрілка
        getChildren().addAll(mainLine, arrowHead);

        // Перший виклик
        updateArrow();
    }

    /**
     * Якщо треба задати проміжні точки (ламана), викликаємо setWaypoints(...).
     */
    public void setWaypoints(List<Point2D> pts) {
        this.waypoints.clear();
        if (pts != null && !pts.isEmpty()) {
            this.waypoints.addAll(pts);

            if (!getChildren().contains(polyline)) {
                // Додаємо полілайн у Group (наприклад, на нульову позицію – позаду mainLine)
                getChildren().add(0, polyline);
            }
            updatePolyline();
        } else {
            getChildren().remove(polyline);
        }
    }

    private void updatePolyline() {
        if (waypoints.isEmpty()) {
            getChildren().remove(polyline);
            return;
        }

        double sx = mainLine.getStartX();
        double sy = mainLine.getStartY();
        double tx = mainLine.getEndX();
        double ty = mainLine.getEndY();

        List<Double> coords = new ArrayList<>();
        coords.add(sx); coords.add(sy);
        for (Point2D p : waypoints) {
            coords.add(p.getX());
            coords.add(p.getY());
        }
        coords.add(tx); coords.add(ty);

        polyline.getPoints().setAll(coords);
    }

    /**
     * Оновлює позицію стрілки. Якщо TargetNode — SERVICE_DEVICE, беремо
     * його справжній центр кола і відступаємо від нього на радіус (r).
     */
    private void updateArrow() {
        double sx = mainLine.getStartX();
        double sy = mainLine.getStartY();
        double tx = mainLine.getEndX();
        double ty = mainLine.getEndY();

        // Якщо targetNode == SERVICE_DEVICE, беремо глобальний центр кола тощо
        if (targetNode.getNodeType() == NodeType.SERVICE_DEVICE && targetNode.getDeviceCircle() != null) {
            tx = targetNode.getGlobalCenterX();
            ty = targetNode.getGlobalCenterY();
        }

        double dx = tx - sx;
        double dy = ty - sy;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double length = Math.sqrt(dx*dx + dy*dy);

        // Визначаємо відступ залежно від типу targetNode
        double offset;
        NodeType ttype = targetNode.getNodeType();
        switch (ttype) {
            case SERVICE_DEVICE:
                // Якщо коло — беремо радіус. (Якщо треба "врізатись" - r-2, якщо "не доходити" - r+2)
                double r = targetNode.getRadiusIfServiceDevice();
                offset = (r > 0) ? r : 30.0;
                break;

            case ROUTE_SPLIT:
                // Розгалуження (кружечок маленького радіуса) – зменшимо відступ, напр. 10
                offset = 5.0;
                break;

            case QUEUE:
                // Черга (картинка) – трохи збільшимо, напр. 40, щоб не лізла під зображення
                offset = 35.0;
                break;

            default:
                // Інакше, нехай буде 30
                offset = 30.0;
        }

        double factor = offset / length;
        double xA = tx - dx * factor;
        double yA = ty - dy * factor;

        arrowHead.setTranslateX(xA);
        arrowHead.setTranslateY(yA);
        arrowHead.setRotate(angle);
    }
    // Геттери/сеттери
    public DraggableNode getSourceNode() {
        return sourceNode;
    }
    public DraggableNode getTargetNode() {
        return targetNode;
    }

    public double getProbability() {
        return probability;
    }
    public void setProbability(double probability) {
        this.probability = probability;
    }
}