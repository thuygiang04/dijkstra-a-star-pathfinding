import java.io.*;
import java.util.*;

public class Graph {
    private final Map<Long, Node> nodes = new HashMap<>();
    private final Map<Long, List<Edge>> adj = new HashMap<>();

    // ---------- Kết quả tìm đường ----------
    public static class Result {
        public final List<Long> path;   // rỗng nếu không có đường
        public final double distance;   // mét
        public final int visited;       // số nút đã duyệt
        public final long millis;

        Result(List<Long> path, double distance, int visited, long millis) {
            this.path = path;
            this.distance = distance;
            this.visited = visited;
            this.millis = millis;
        }
    }

    private static class QItem {
        final long id;
        final double f;   // độ ưu tiên: g (Dijkstra) hoặc g + h (A*)
        QItem(long id, double f) { this.id = id; this.f = f; }
    }

    // ---------- Đọc dữ liệu CSV ----------
    // nodes.csv: osmid,y,x     edges.csv: u,v,key,length
    public void loadNodes(String file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String> header = Arrays.asList(br.readLine().trim().split(","));
            int iId = header.indexOf("osmid"), iLat = header.indexOf("y"), iLon = header.indexOf("x");
            if (iId < 0) iId = 0;
            if (iLat < 0) iLat = 1;
            if (iLon < 0) iLon = 2;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                long id = Long.parseLong(p[iId].trim());
                nodes.put(id, new Node(id, Double.parseDouble(p[iLat]), Double.parseDouble(p[iLon])));
                adj.putIfAbsent(id, new ArrayList<>());
            }
        }
    }

    public void loadEdges(String file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String> header = Arrays.asList(br.readLine().trim().split(","));
            int iU = header.indexOf("u"), iV = header.indexOf("v"), iW = header.indexOf("length");
            if (iU < 0) iU = 0;
            if (iV < 0) iV = 1;
            if (iW < 0) iW = header.size() - 1;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                long u = Long.parseLong(p[iU].trim()), v = Long.parseLong(p[iV].trim());
                double w = Double.parseDouble(p[iW]);
                // đồ thị có hướng: đường một chiều chỉ có cạnh u -> v
                adj.computeIfAbsent(u, k -> new ArrayList<>()).add(new Edge(v, w));
            }
        }
    }

    public Node getNode(long id) { return nodes.get(id); }
    public boolean hasNode(long id) { return nodes.containsKey(id); }
    public int nodeCount() { return nodes.size(); }
    public int edgeCount() { return adj.values().stream().mapToInt(List::size).sum(); }
    public Collection<Node> allNodes() { return nodes.values(); }
    public List<Edge> neighbors(long id) { return adj.getOrDefault(id, Collections.emptyList()); }

    // ---------- Khoảng cách Haversine (mét) ----------
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    // ---------- Nút gần tọa độ nhập vào nhất ----------
    public Node nearest(double lat, double lon) {
        Node best = null;
        double bestD = Double.MAX_VALUE;
        for (Node n : nodes.values()) {
            double d = haversine(lat, lon, n.lat, n.lon);
            if (d < bestD) { bestD = d; best = n; }
        }
        return best;
    }

    // ---------- Dijkstra (useAStar = false) hoặc A* (useAStar = true) ----------
    public Result shortestPath(long start, long end, boolean useAStar) {
        long t0 = System.currentTimeMillis();
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        Set<Long> done = new HashSet<>();
        PriorityQueue<QItem> pq = new PriorityQueue<>(Comparator.comparingDouble((QItem q) -> q.f));
        Node target = nodes.get(end);

        dist.put(start, 0.0);
        pq.add(new QItem(start, 0.0));

        while (!pq.isEmpty()) {
            long u = pq.poll().id;
            if (!done.add(u)) continue;          // đã xử lý rồi
            if (u == end) break;

            double du = dist.get(u);
            for (Edge e : neighbors(u)) {
                double nd = du + e.weight;
                if (nd < dist.getOrDefault(e.to, Double.MAX_VALUE)) {
                    dist.put(e.to, nd);
                    prev.put(e.to, u);
                    double h = 0;
                    if (useAStar) {
                        Node v = nodes.get(e.to);
                        h = haversine(v.lat, v.lon, target.lat, target.lon);  // đường chim bay <= đường thực
                    }
                    pq.add(new QItem(e.to, nd + h));
                }
            }
        }

        long ms = System.currentTimeMillis() - t0;
        if (!dist.containsKey(end)) return new Result(new ArrayList<>(), Double.POSITIVE_INFINITY, done.size(), ms);

        LinkedList<Long> path = new LinkedList<>();
        for (Long at = end; at != null; at = prev.get(at)) path.addFirst(at);
        return new Result(path, dist.get(end), done.size(), ms);
    }
}
