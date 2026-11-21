package ru.example.library.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import ru.example.library.store.NodeSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** JavaFX Canvas для отображения формы BST и посещённых при последней операции узлов. */
public final class BstCanvasView extends Canvas {
    private static final double NODE_RADIUS = 23;
    private List<NodeSnapshot> nodes = List.of();
    private List<String> highlightedTitles = List.of();

    public BstCanvasView(double width, double height) {
        super(width, height);
        widthProperty().addListener((o, a, b) -> draw());
        heightProperty().addListener((o, a, b) -> draw());
    }

    public void render(List<NodeSnapshot> snapshot, List<String> highlighted) {
        nodes = List.copyOf(snapshot);
        highlightedTitles = List.copyOf(highlighted);
        draw();
    }

    private void draw() {
        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth(), h = getHeight();
        g.clearRect(0, 0, w, h);
        g.setFill(Color.web("#f8fafc")); g.fillRect(0, 0, w, h);
        if (nodes.isEmpty()) {
            g.setFill(Color.GRAY); g.setFont(Font.font(14));
            g.fillText("Дерево пусто — добавьте или загрузите книги", 18, 30);
            return;
        }
        Map<Integer, Integer> depths = new HashMap<>();
        Map<Integer, Double> xPositions = new HashMap<>();
        int[] cursor = {0};
        assignPositions(0, 0, depths, xPositions, cursor, w);
        double maxDepth = depths.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        double verticalGap = Math.max(72, (h - 70) / (maxDepth + 1));

        g.setStroke(Color.web("#94a3b8")); g.setLineWidth(1.5);
        for (int i = 0; i < nodes.size(); i++) {
            NodeSnapshot node = nodes.get(i);
            if (node.parentIndex() >= 0) {
                double x1 = xPositions.get(node.parentIndex());
                double y1 = 35 + depths.get(node.parentIndex()) * verticalGap;
                double x2 = xPositions.get(i);
                double y2 = 35 + depths.get(i) * verticalGap;
                g.strokeLine(x1, y1, x2, y2);
            }
        }
        for (int i = 0; i < nodes.size(); i++) {
            NodeSnapshot node = nodes.get(i);
            double x = xPositions.get(i), y = 35 + depths.get(i) * verticalGap;
            boolean active = highlightedTitles.stream().anyMatch(t -> t.equalsIgnoreCase(node.title()));
            g.setFill(active ? Color.web("#f59e0b") : Color.web("#dbeafe"));
            g.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            g.setStroke(Color.web("#334155")); g.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            g.setFill(Color.web("#0f172a")); g.setFont(Font.font(10));
            String label = node.title().length() > 13 ? node.title().substring(0, 12) + "…" : node.title();
            g.fillText(label, x - Math.min(55, label.length() * 3.1), y + NODE_RADIUS + 13);
        }
    }

    private void assignPositions(int index, int depth, Map<Integer, Integer> depths,
                                 Map<Integer, Double> xPositions, int[] cursor, double width) {
        if (index < 0 || index >= nodes.size()) return;
        // Snapshot is preorder; derive child indexes by parentIndex and side.
        int left = child(index, true), right = child(index, false);
        assignPositions(left, depth + 1, depths, xPositions, cursor, width);
        depths.put(index, depth);
        xPositions.put(index, (cursor[0] + 1) * width / (nodes.size() + 1));
        cursor[0]++;
        assignPositions(right, depth + 1, depths, xPositions, cursor, width);
    }

    private int child(int parent, boolean left) {
        for (int i = parent + 1; i < nodes.size(); i++) {
            NodeSnapshot candidate = nodes.get(i);
            if (candidate.parentIndex() == parent && candidate.isLeftChild() == left) return i;
        }
        return -1;
    }
}
