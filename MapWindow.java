import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Hiển thị bản đồ giống bản Python:
 *  - Nền xám: toàn bộ đường; đường đi: xanh dương; bắt đầu: xanh lá; kết thúc: đỏ.
 *  - Click chuột chọn điểm bắt đầu rồi điểm kết thúc (click lần 3 để chọn lại).
 *  - Kéo chuột để di chuyển, lăn chuột để phóng to/thu nhỏ.
 */
public class MapWindow extends Application {
    private Graph g;
    private Canvas canvas = new Canvas();
    private double minLat = 1e9, maxLat = -1e9, minLon = 1e9, maxLon = -1e9, cosLat;
    private double scale = 0, offX, offY;          // phép chiếu: màn hình = thế giới * scale + off
    private double lastX, lastY;
    private boolean dragged;

    private Node src, dst;                          // nút bắt đầu / kết thúc
    private Graph.Result result;

    private final ComboBox<String> algo = new ComboBox<>();
    private final CheckBox showWeights = new CheckBox("Hiện trọng số (mét)");
    private final CheckBox showDirections = new CheckBox("Hiện hướng đường");
    private final Label info = new Label("Click lên bản đồ để chọn điểm bắt đầu.");

    @Override
    public void start(Stage stage) throws Exception {
        List<String> a = getParameters().getRaw();
        g = new Graph();
        g.loadNodes(a.size() > 0 ? a.get(0) : "nodes.csv");
        g.loadEdges(a.size() > 1 ? a.get(1) : "edges.csv");

        for (Node n : g.allNodes()) {
            minLat = Math.min(minLat, n.lat); maxLat = Math.max(maxLat, n.lat);
            minLon = Math.min(minLon, n.lon); maxLon = Math.max(maxLon, n.lon);
        }
        cosLat = Math.cos(Math.toRadians((minLat + maxLat) / 2));

        algo.getItems().addAll("Dijkstra", "A*");
        algo.getSelectionModel().selectFirst();
        algo.setOnAction(e -> { compute(); draw(); });
        showWeights.setOnAction(e -> draw());
        showDirections.setSelected(true);
        showDirections.setTooltip(new Tooltip("Một mũi tên: một chiều. Hai mũi tên ngược chiều: hai chiều."));
        showDirections.setOnAction(e -> draw());
        Button reset = new Button("Chọn lại");
        reset.setOnAction(e -> { src = dst = null; result = null; info.setText("Click lên bản đồ để chọn điểm bắt đầu."); draw(); });

        HBox bar = new HBox(10, new Label("Thuật toán:"), algo, showWeights, showDirections, reset, info);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setMaxWidth(Double.MAX_VALUE);
        bar.setPadding(new Insets(8));

        Pane center = new Pane(canvas);
        canvas.widthProperty().bind(center.widthProperty());
        canvas.heightProperty().bind(center.heightProperty());
        canvas.widthProperty().addListener(o -> draw());
        canvas.heightProperty().addListener(o -> draw());

        canvas.setOnMousePressed(e -> { lastX = e.getX(); lastY = e.getY(); dragged = false; });
        canvas.setOnMouseDragged(e -> {
            offX += e.getX() - lastX; offY += e.getY() - lastY;
            if (Math.abs(e.getX() - lastX) + Math.abs(e.getY() - lastY) > 1) dragged = true;
            lastX = e.getX(); lastY = e.getY();
            draw();
        });
        canvas.setOnMouseReleased(e -> { if (!dragged) pick(e.getX(), e.getY()); });
        canvas.setOnScroll(e -> {
            double f = e.getDeltaY() > 0 ? 1.2 : 1 / 1.2;
            offX = e.getX() - (e.getX() - offX) * f;
            offY = e.getY() - (e.getY() - offY) * f;
            scale *= f;
            draw();
        });

        BorderPane root = new BorderPane(center);
        root.setTop(bar);
        stage.setScene(new Scene(root, 1100, 800));
        stage.setTitle("Tìm đường ngắn nhất - " + g.nodeCount() + " nút, " + g.edgeCount() + " cạnh");
        stage.show();
    }

    // ---------- Chuyển đổi tọa độ ----------
    private double sx(Node n) { return (n.lon - minLon) * cosLat * scale + offX; }
    private double sy(Node n) { return (maxLat - n.lat) * scale + offY; }

    private void fit() {
        double w = (maxLon - minLon) * cosLat, h = maxLat - minLat;
        scale = Math.min(canvas.getWidth() / w, canvas.getHeight() / h) * 0.95;
        offX = (canvas.getWidth() - w * scale) / 2;
        offY = (canvas.getHeight() - h * scale) / 2;
    }

    // ---------- Chọn điểm bằng chuột ----------
    private void pick(double x, double y) {
        double lon = (x - offX) / scale / cosLat + minLon;
        double lat = maxLat - (y - offY) / scale;
        Node n = g.nearest(lat, lon);
        if (src == null || dst != null) {
            src = n; dst = null; result = null;
            info.setText("Bắt đầu: " + n.id + ". Click để chọn điểm kết thúc.");
        } else {
            dst = n;
            compute();
        }
        draw();
    }

    private void compute() {
        if (src == null || dst == null) return;
        Graph.Result d = g.shortestPath(src.id, dst.id, false);
        Graph.Result a = g.shortestPath(src.id, dst.id, true);
        result = algo.getSelectionModel().getSelectedIndex() == 1 ? a : d;
        if (result.path.isEmpty()) {
            info.setText("Không có đường đi (đường một chiều hoặc không liên thông).");
        } else {
            info.setText(String.format("%.0f m | Dijkstra: duyệt %d nút, %d ms | A*: duyệt %d nút, %d ms",
                    result.distance, d.visited, d.millis, a.visited, a.millis));
        }
    }

    // ---------- Vẽ ----------
    private void draw() {
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) return;
        if (scale == 0) fit();
        double W = canvas.getWidth(), H = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, W, H);

        // 1. Toàn bộ đường (nền xám) + trọng số
        gc.setStroke(Color.web("#bbbbbb"));
        gc.setLineWidth(0.8);
        gc.setFont(Font.font(10));
        for (Node u : g.allNodes()) {
            for (Edge e : g.neighbors(u.id)) {
                Node v = g.getNode(e.to);
                double x1 = sx(u), y1 = sy(u), x2 = sx(v), y2 = sy(v);
                if (Math.max(x1, x2) < 0 || Math.min(x1, x2) > W || Math.max(y1, y2) < 0 || Math.min(y1, y2) > H) continue;
                gc.strokeLine(x1, y1, x2, y2);

                boolean longEnough = Math.hypot(x2 - x1, y2 - y1) > 40;   // tránh nhãn dày đặc
                if (showWeights.isSelected() && longEnough && (u.id < v.id || !hasEdge(v.id, u.id))) {
                    gc.setFill(Color.BLUE);
                    gc.fillText(String.format("%.0f", e.weight), (x1 + x2) / 2, (y1 + y2) / 2);
                }
            }
        }

        // 2. Đường đi
        if (result != null && !result.path.isEmpty()) {
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(4);
            List<Long> p = result.path;
            for (int i = 0; i + 1 < p.size(); i++) {
                Node a = g.getNode(p.get(i)), b = g.getNode(p.get(i + 1));
                gc.strokeLine(sx(a), sy(a), sx(b), sy(b));
            }
        }

        if (showDirections.isSelected()) {
            drawDirections(gc);
            drawDirectionLegend(gc, W, H);
        }

        // 3. Điểm bắt đầu / kết thúc
        if (src != null) { gc.setFill(Color.GREEN); gc.fillOval(sx(src) - 7, sy(src) - 7, 14, 14); }
        if (dst != null) { gc.setFill(Color.RED);   gc.fillOval(sx(dst) - 7, sy(dst) - 7, 14, 14); }
    }

    private boolean hasEdge(long from, long to) {
        for (Edge e : g.neighbors(from)) if (e.to == to) return true;
        return false;
    }

    private void drawDirections(GraphicsContext gc) {
        Set<String> drawn = new HashSet<>();
        gc.setFill(Color.web("#34495e"));
        for (Node from : g.allNodes()) {
            for (Edge edge : g.neighbors(from.id)) {
                Node to = g.getNode(edge.to);
                if (to == null) continue;

                boolean bidirectional = hasEdge(to.id, from.id);
                String key;
                if (bidirectional) {
                    long low = Math.min(from.id, to.id), high = Math.max(from.id, to.id);
                    key = "B:" + low + ":" + high;
                } else {
                    key = "D:" + from.id + ":" + to.id;
                }
                if (!drawn.add(key)) continue;

                if (bidirectional) {
                    drawArrow(gc, from, to, 0.35);
                    drawArrow(gc, to, from, 0.35);
                } else {
                    drawArrow(gc, from, to, 0.5);
                }
            }
        }
    }

    private void drawArrow(GraphicsContext gc, Node from, Node to, double position) {
        double x1 = sx(from), y1 = sy(from), x2 = sx(to), y2 = sy(to);
        double dx = x2 - x1, dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        if (length < 22) return;

        double ux = dx / length, uy = dy / length;
        double px = -uy, py = ux;
        double tipX = x1 + dx * position, tipY = y1 + dy * position;
        double size = 7;
        double baseX = tipX - ux * size, baseY = tipY - uy * size;
        double halfWidth = size * 0.58;
        gc.fillPolygon(
                new double[]{tipX, baseX + px * halfWidth, baseX - px * halfWidth},
                new double[]{tipY, baseY + py * halfWidth, baseY - py * halfWidth},
                3);
    }

    private void drawDirectionLegend(GraphicsContext gc, double width, double height) {
        String legend = "→ một chiều     ↔ hai chiều";
        gc.setFont(Font.font(12));
        double textWidth = 160;
        double x = Math.max(8, width - textWidth - 16);
        double y = height - 16;
        gc.setFill(Color.rgb(255, 255, 255, 0.88));
        gc.fillRoundRect(x - 6, y - 15, textWidth + 12, 22, 6, 6);
        gc.setFill(Color.web("#34495e"));
        gc.fillText(legend, x, y);
    }

}
