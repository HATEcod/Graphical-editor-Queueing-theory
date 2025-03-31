package org.example.graphic_editor;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class NodeParameterDialog extends Dialog<NodeParameterDialog.ResultData> {

    // Поля для Generator/ServiceDevice (мін/макс час)
    private TextField timeDelayMinField;
    private TextField timeDelayMaxField;

    // Поле для Queue
    private TextField queueMaxField;

    // Поля для RouteSplit
    private TextField p1Field;
    private TextField p2Field;
    private TextField p3Field;

    private DraggableNode node;

    public NodeParameterDialog(DraggableNode node) {
        this.node = node;

        setTitle("Налаштування: " + node.getNodeType());
        setHeaderText("Задайте параметри вузла");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Відмінити", ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        NodeType type = node.getNodeType();
        int rowIndex = 0;

        switch (type) {
            case GENERATOR:
            case SERVICE_DEVICE:
                Label lblMin = new Label("Мінімальна затримка (min):");
                timeDelayMinField = new TextField(
                        String.valueOf(Optional.ofNullable(node.getTimeDelayMin()).orElse(1.0))
                );
                Label lblMax = new Label("Максимальна затримка (max):");
                timeDelayMaxField = new TextField(
                        String.valueOf(Optional.ofNullable(node.getTimeDelayMax()).orElse(3.0))
                );

                grid.add(lblMin, 0, rowIndex);
                grid.add(timeDelayMinField, 1, rowIndex);
                rowIndex++;

                grid.add(lblMax, 0, rowIndex);
                grid.add(timeDelayMaxField, 1, rowIndex);
                rowIndex++;
                break;

            case QUEUE:
                Label lblQ = new Label("Максимальна довжина (max):");
                queueMaxField = new TextField(
                        (node.getQueueMax() == null) ? "" : node.getQueueMax().toString()
                );
                grid.add(lblQ, 0, rowIndex);
                grid.add(queueMaxField, 1, rowIndex);

                break;

            case ROUTE_SPLIT:
                Label lblP1 = new Label("p1:");
                p1Field = new TextField("0.5");
                Label lblP2 = new Label("p2:");
                p2Field = new TextField("0.5");
                Label lblP3 = new Label("p3:");
                p3Field = new TextField("0.0");

                grid.add(lblP1, 0, rowIndex);
                grid.add(p1Field, 1, rowIndex);
                rowIndex++;
                grid.add(lblP2, 0, rowIndex);
                grid.add(p2Field, 1, rowIndex);
                rowIndex++;
                grid.add(lblP3, 0, rowIndex);
                grid.add(p3Field, 1, rowIndex);
                rowIndex++;
                break;
        }

        getDialogPane().setContent(grid);

        // Обробка "OK"
        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                ResultData rd = new ResultData();

                switch (type) {
                    case GENERATOR:
                    case SERVICE_DEVICE:
                        rd.setTimeDelayMin(parseDoubleSafe(timeDelayMinField.getText(), 1.0));
                        rd.setTimeDelayMax(parseDoubleSafe(timeDelayMaxField.getText(), 3.0));
                        break;
                    case QUEUE:
                        rd.setQueueMax(parseIntSafe(queueMaxField.getText(), -1));
                        break;
                    case ROUTE_SPLIT:
                        rd.setP1(parseDoubleSafe(p1Field.getText(), 0.5));
                        rd.setP2(parseDoubleSafe(p2Field.getText(), 0.5));
                        rd.setP3(parseDoubleSafe(p3Field.getText(), 0.0));
                        // Потом допишу перевірку sum=1,
                        break;
                }
                return rd;
            }
            return null;
        });
    }

    private double parseDoubleSafe(String s, double defVal) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return defVal;
        }
    }
    private int parseIntSafe(String s, int defVal) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public static class ResultData {
        private Double timeDelayMin;
        private Double timeDelayMax;
        private Integer queueMax;
        private Double p1, p2, p3;

        public Double getTimeDelayMin() { return timeDelayMin; }
        public void setTimeDelayMin(Double v) { timeDelayMin = v; }
        public Double getTimeDelayMax() { return timeDelayMax; }
        public void setTimeDelayMax(Double v) { timeDelayMax = v; }

        public Integer getQueueMax() { return queueMax; }
        public void setQueueMax(Integer q) { queueMax=q; }

        public Double getP1() { return p1; }
        public Double getP2() { return p2; }
        public Double getP3() { return p3; }
        public void setP1(Double v) { p1=v; }
        public void setP2(Double v) { p2=v; }
        public void setP3(Double v) { p3=v; }

    }
}
