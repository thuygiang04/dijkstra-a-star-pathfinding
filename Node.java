public class Node {
    public final long id;
    public final double lat;   // vĩ độ (cột y)
    public final double lon;   // kinh độ (cột x)

    public Node(long id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    public String toString() {
        return id + " (" + lat + ", " + lon + ")";
    }
}
