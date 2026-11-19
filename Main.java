import java.io.*;
import java.util.*;

public class Main {
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        String nodesFile = args.length > 0 ? args[0] : "nodes.csv";
        String edgesFile = args.length > 1 ? args[1] : "edges.csv";

        Graph g = new Graph();
        g.loadNodes(nodesFile);
        g.loadEdges(edgesFile);
        System.out.println("Đã đọc " + g.nodeCount() + " nút, " + g.edgeCount() + " cạnh.");

        while (true) {
            long start = readPoint(g, "điểm bắt đầu");
            long end = readPoint(g, "điểm kết thúc");

            System.out.print("Thuật toán (1 = Dijkstra, 2 = A*, 3 = so sánh cả hai): ");
            String mode = sc.nextLine().trim();

            if (mode.equals("3")) {
                print(g, "Dijkstra", g.shortestPath(start, end, false));
                print(g, "A*      ", g.shortestPath(start, end, true));
            } else {
                boolean astar = mode.equals("2");
                Graph.Result r = g.shortestPath(start, end, astar);
                print(g, astar ? "A*" : "Dijkstra", r);
                if (!r.path.isEmpty()) saveRoute(g, r);
            }

            System.out.print("Tìm tiếp? (y/n): ");
            if (!sc.nextLine().trim().equalsIgnoreCase("y")) break;
        }
    }

    // Nhập "vĩ độ, kinh độ" (ví dụ 21.0285, 105.8522) hoặc ID nút
    static long readPoint(Graph g, String label) {
        while (true) {
            System.out.print("Nhập " + label + " (vĩ độ, kinh độ hoặc ID nút): ");
            String s = sc.nextLine().trim();
            try {
                if (s.contains(",")) {
                    String[] p = s.split(",");
                    double lat = Double.parseDouble(p[0].trim()), lon = Double.parseDouble(p[1].trim());
                    Node n = g.nearest(lat, lon);
                    double off = Graph.haversine(lat, lon, n.lat, n.lon);
                    System.out.printf("  -> nút %d (lệch %.0f m so với điểm bạn nhập)%n", n.id, off);
                    return n.id;
                }
                long id = Long.parseLong(s);
                if (g.hasNode(id)) return id;
                System.out.println("  ID nút không tồn tại.");
            } catch (Exception e) {
                System.out.println("  Dữ liệu nhập không hợp lệ, thử lại.");
            }
        }
    }

    static void print(Graph g, String name, Graph.Result r) {
        if (r.path.isEmpty()) {
            System.out.println(name + ": không có đường đi (đường một chiều hoặc đồ thị không liên thông).");
            return;
        }
        System.out.printf("%s: %.0f m | %d nút trên đường đi | duyệt %d nút | %d ms%n",
                name, r.distance, r.path.size(), r.visited, r.millis);
    }

    // Xuất đường đi ra route.csv (lat,lon) để vẽ lại bằng Python hoặc JavaFX
    static void saveRoute(Graph g, Graph.Result r) throws IOException {
        try (PrintWriter pw = new PrintWriter("route.csv")) {
            pw.println("osmid,lat,lon");
            for (long id : r.path) {
                Node n = g.getNode(id);
                pw.println(id + "," + n.lat + "," + n.lon);
            }
        }
        System.out.println("Đã lưu đường đi vào route.csv");
    }
}
