package org.example.graphic_editor;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.Dragboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * MainController керує робочою зоною (editorPane) й панеллю інструментів (toolbox),
 * реалізує можливість додавання вузлів, створення дуг (прямих або ламаних),
 * і синхронізує це з SimulationEngine.
 */
public class MainController {

    private Pane editorPane;           // Панель для розміщення вузлів і дуг
    private VBox toolbox;              // Ліва панель із кнопками/полями

    private TextField maxTimeField;    // Поле введення maxTime для симуляції
    private ToggleButton createArcToggle; // Кнопка-перемикач: режим створення дуг
    private ToggleButton dashedArcToggle;
    private boolean arcCreationMode = false;
    private DraggableNode selectedNodeForArc = null; // Який вузол обрано як source

    // Проміжні точки, якщо користувач клацає на порожньому полі
    private List<Point2D> tempWayPoints = new ArrayList<>();
    // Ламана, яка відображається під час "проклацування" шляху
    private Polyline tempPolyline = null;

    private List<DraggableNode> allNodes;
    private List<ArcLine> arcs;
    private SimulationEngine engine;
    private boolean simulationInitialized = false;

    public MainController() {
        // Створюємо editorPane
        editorPane = new Pane();
        editorPane.setPrefSize(900, 600);
        editorPane.setStyle("-fx-background-color: #f0f0f0;");

        // Створюємо toolbox
        toolbox = new VBox();
        toolbox.setSpacing(10);
        toolbox.setPadding(new Insets(10));

        // Ініціалізуємо списки
        allNodes = new ArrayList<>();
        arcs = new ArrayList<>();
        engine = new SimulationEngine();

        // Кнопки для різних типів вузлів
        Button genBtn = new Button("Генератор");
        Button queueBtn = new Button("Черга");
        Button devBtn = new Button("Пристрій");
        Button splitBtn = new Button("Розгалуження");

        // Налаштовуємо drag&drop
        setupDragAndDropSource(genBtn, "Генератор");
        setupDragAndDropSource(queueBtn, "Черга");
        setupDragAndDropSource(devBtn, "Пристрій обслуговування");
        setupDragAndDropSource(splitBtn, "Розгалуження");

        // Кнопка-перемикач створення дуг
        createArcToggle = new ToggleButton("Дуга");
        createArcToggle.setOnAction(e -> {
            arcCreationMode = createArcToggle.isSelected();
            if (!arcCreationMode) {
                clearTempPath();
                selectedNodeForArc = null;
            }
        });
        // Кнопка для створення дуги блокування
        dashedArcToggle = new ToggleButton("Дуга блокування");
        toolbox.getChildren().add(dashedArcToggle);

        // Поле введення maxTime
        Label timeLbl = new Label("Макс час (maxTime):");
        maxTimeField = new TextField("50");
        HBox timeBox = new HBox(timeLbl, maxTimeField);
        timeBox.setSpacing(5);

        // Кнопки симуляції
        Button simStepBtn = new Button("Sim Step");
        simStepBtn.setOnAction(e -> {
            if (!simulationInitialized) {
                setEngineMaxTime();
                engine.initSimulation();
                simulationInitialized = true;
            }
            engine.runOneEvent();
        });

        Button runFullBtn = new Button("Run Full");
        runFullBtn.setOnAction(e -> {
            if (!simulationInitialized) {
                setEngineMaxTime();
                engine.initSimulation();
                simulationInitialized = true;
            }
            engine.runSimulation();
        });

        // Додаємо все у toolbox
        toolbox.getChildren().addAll(
                genBtn, queueBtn, devBtn, splitBtn,
                createArcToggle, timeBox, simStepBtn, runFullBtn
        );

        // Налаштовуємо editorPane для прийому вузлів
        setupEditorPaneDragAndDrop(editorPane);

        // Клік по порожньому полі => додавання проміжних точок
        editorPane.setOnMouseClicked(evt -> {
            if (!arcCreationMode) return;
            if (selectedNodeForArc != null) {
                Point2D p = new Point2D(evt.getX(), evt.getY());
                tempWayPoints.add(p);
                updateTempPolyline();
            }
        });
    }

    /**
     * Повертає editorPane, щоб додати в Scene
     */
    public Pane getEditorPane() {
        return editorPane;
    }

    /**
     * Повертає toolbox, щоб розмістити ліворуч
     */
    public VBox getToolbox() {
        return toolbox;
    }

    /**
     * Зчитуємо maxTime з поля та виставляємо в engine
     */
    private void setEngineMaxTime() {
        String text = maxTimeField.getText();
        double val;
        try {
            val = Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            val = 50.0;
        }
        engine.setMaxTime(val);
        System.out.println("[MainController] maxTime = " + val);
    }

    /**
     * Налаштовує кнопку, щоб під час dragDetected починався перенос текстового типу (objectType)
     */
    private void setupDragAndDropSource(Button button, String objectType) {
        button.setOnDragDetected(evt -> {
            Dragboard db = button.startDragAndDrop(TransferMode.COPY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(objectType);
            db.setContent(cc);
            evt.consume();
        });
    }

    /**
     * Дозволяємо editorPane приймати "скинуті" вузли
     */
    private void setupEditorPaneDragAndDrop(Pane pane) {
        pane.setOnDragOver(evt -> {
            if (evt.getGestureSource() != pane && evt.getDragboard().hasString()) {
                evt.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            evt.consume();
        });

        pane.setOnDragDropped(evt -> {
            Dragboard db = evt.getDragboard();
            if (db.hasString()) {
                DraggableNode node = new DraggableNode(db.getString());
                node.setLayoutX(evt.getX());
                node.setLayoutY(evt.getY());

                // Обробник клацань
                node.setOnMouseClicked(me -> {
                    if (me.getButton() == MouseButton.SECONDARY) {
                        // Правий клік => редагувати параметри вузла
                        NodeParameterDialog dialog = new NodeParameterDialog(node);
                        dialog.showAndWait().ifPresent(res -> {
                            node.applyDialogResult(res);
                        });
                        me.consume();
                    } else if (me.getButton() == MouseButton.PRIMARY) {
                        // Лівий клік => створення дуг
                        if (arcCreationMode && me.getClickCount() == 1) {
                            handleCreateArcClick(node);
                            me.consume();
                        }
                    }
                });

                // Додаємо вузол у pane і в списки
                // (Додаємо наприкінці, щоб вузол був поверх ліній)
                pane.getChildren().add(node);
                allNodes.add(node);
                engine.addNode(node);

                evt.setDropCompleted(true);
            } else {
                evt.setDropCompleted(false);
            }
            evt.consume();
        });
    }

    /**
     * Якщо SourceNode ще не обрано => обираємо даний (clickedNode);
     * Інакше (SourceNode є) => це буде TargetNode => створюємо дугу.
     */
    private void handleCreateArcClick(DraggableNode clickedNode) {
        if (selectedNodeForArc == null) {
            // Вибираємо source
            selectedNodeForArc = clickedNode;
            clearTempPath();
            updateTempPolyline();
        } else {
            // Маємо source => тепер target
            if (selectedNodeForArc != clickedNode) {
                // Якщо активовано режим пунктирних дуг через dashedArcToggle
                if (dashedArcToggle.isSelected()) {
                    if (tempWayPoints.isEmpty()) {
                        // Якщо немає проміжних точок – створюємо звичайну дугу (ArcLine)
                        ArcLine arc = new ArcLine(selectedNodeForArc, clickedNode, 1.0);
                        arcs.add(arc);
                        engine.addArc(arc);
                        editorPane.getChildren().add(0, arc);
                    } else {
                        // Якщо є проміжні точки – створюємо дугу з пунктирною лінією з draggable‑waypoint точками
                        DashedPolylineArc dashedArc = new DashedPolylineArc(selectedNodeForArc, clickedNode, tempWayPoints);
                        editorPane.getChildren().add(0, dashedArc);
                    }
                } else {
                    // Старий функціонал створення дуг
                    ArcLine arc = new ArcLine(selectedNodeForArc, clickedNode, 1.0);
                    arcs.add(arc);
                    engine.addArc(arc);

                    // Якщо queue -> device, встановлюємо pull
                    if (selectedNodeForArc.getNodeType() == NodeType.QUEUE
                            && clickedNode.getNodeType() == NodeType.SERVICE_DEVICE) {
                        clickedNode.setInputQueueNode(selectedNodeForArc);
                    }

                    // Якщо немає проміжних точок – додаємо звичайну дугу, інакше – ламану дугу
                    if (tempWayPoints.isEmpty()) {
                        editorPane.getChildren().add(0, arc);
                    } else {
                        PolylineArc polyArc = new PolylineArc(selectedNodeForArc, clickedNode, tempWayPoints);
                        editorPane.getChildren().add(0, polyArc);
                    }
                }
            }
            // Скидаємо вибір та очищуємо тимчасовий шлях
            selectedNodeForArc = null;
            clearTempPath();
        }
    }

    /**
     * Очищаємо тимчасовий полілайн і список проміжних точок
     */
    private void clearTempPath() {
        if (tempPolyline != null) {
            editorPane.getChildren().remove(tempPolyline);
            tempPolyline = null;
        }
        tempWayPoints.clear();
    }

    /**
     * Оновити (або створити) tempPolyline від центра SourceNode до всіх проміжних точок
     */
    private void updateTempPolyline() {
        if (selectedNodeForArc == null) {
            return;
        }
        if (tempPolyline == null) {
            tempPolyline = new Polyline();
            tempPolyline.setStrokeWidth(2.0);
            // Щоб тимчасова лінія була позаду всіх вузлів, додаємо її на початку
            editorPane.getChildren().add(0, tempPolyline);
        }

        double sx = selectedNodeForArc.getLayoutX() + selectedNodeForArc.getWidth() / 2.0;
        double sy = selectedNodeForArc.getLayoutY() + selectedNodeForArc.getHeight() / 2.0;

        List<Double> coords = new ArrayList<>();
        coords.add(sx);
        coords.add(sy);

        for (Point2D point : tempWayPoints) {
            coords.add(point.getX());
            coords.add(point.getY());
        }

        tempPolyline.getPoints().setAll(coords);
    }
}