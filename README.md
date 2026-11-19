# Tìm đường đi ngắn nhất

Ứng dụng Java đọc mạng lưới đường từ hai file CSV và tìm đường ngắn nhất giữa hai vị trí. Project có giao diện bản đồ JavaFX và chương trình dòng lệnh. Các thuật toán được hỗ trợ là Dijkstra và A*.

## Yêu cầu

- JDK 11 trở lên. Cần dùng JDK, không chỉ cài JRE.
- Apache Maven 3.6 trở lên, hoặc Maven tích hợp trong IntelliJ IDEA.
- Windows, macOS hoặc Linux. Maven sẽ tải các thư viện JavaFX phù hợp với hệ điều hành.
- Lần chạy đầu cần kết nối được tới Maven Central để tải dependency.

Project dùng JavaFX `17.0.8`, khai báo trong `pom.xml`. Mã nguồn Java và các file CSV hiện nằm trực tiếp ở thư mục gốc project.

## Chạy bằng IntelliJ IDEA

1. Mở thư mục project có file `pom.xml`.
2. Nếu IntelliJ hỏi có nạp Maven project không, chọn **Load** hoặc **Import**.
3. Mở cửa sổ **Maven** và bấm **Reload All Maven Projects** để tải dependency.
4. Chọn **Plugins → javafx → javafx:run** để chạy giao diện.

Cũng có thể chạy cấu hình **MapApp** nếu cấu hình đó dùng JDK 11 trở lên và Maven dependency đã được nạp. `MapApp` là lớp khởi chạy; cửa sổ JavaFX nằm trong `MapWindow`.

Để chạy chương trình dòng lệnh trong IntelliJ, mở `Main.java` và chạy `Main` bằng cấu hình Application.

## Chạy từ terminal

Mở PowerShell hoặc terminal tại thư mục chứa `pom.xml`.

### Chạy giao diện bản đồ

```powershell
mvn clean javafx:run
```

### Biên dịch project

```powershell
mvn clean compile
```

### Chạy bản dòng lệnh

Sau khi biên dịch:

```powershell
java -cp target/classes Main
```

Có thể truyền đường dẫn đến hai file dữ liệu theo thứ tự nút rồi cạnh:

```powershell
java -cp target/classes Main nodes.csv edges.csv
```

Các đường dẫn tương đối được tính từ thư mục làm việc hiện tại. Nếu chạy từ IntelliJ, đặt **Working directory** là thư mục gốc project để chương trình tìm thấy các file CSV.

## Sử dụng giao diện

1. Khi mở ứng dụng, bản đồ các cạnh được vẽ màu xám.
2. Bấm vào bản đồ để chọn điểm bắt đầu. Ứng dụng dùng nút mạng gần vị trí con trỏ nhất.
3. Bấm lần nữa để chọn điểm kết thúc và tính đường đi.
4. Chọn **Dijkstra** hoặc **A*** trong danh sách để chọn thuật toán hiển thị.
5. Bật **Hiện trọng số (mét)** để hiện độ dài cạnh phù hợp với mức thu phóng.
6. Bật **Hiện hướng đường** để xem chiều lưu thông trên các cạnh. Một mũi tên biểu thị đường một chiều; hai mũi tên ngược chiều biểu thị đường hai chiều. Mũi tên chỉ hiện khi đoạn đường đủ dài trên màn hình để đọc được.
7. Kéo chuột để dịch chuyển bản đồ, lăn chuột để phóng to hoặc thu nhỏ.
8. Bấm **Chọn lại** để xóa lựa chọn hiện tại và bắt đầu lại.

Điểm bắt đầu có màu xanh lá, điểm kết thúc màu đỏ, đường tìm được màu xanh dương. Thanh trạng thái hiển thị độ dài đường đi và thống kê số nút đã duyệt, thời gian chạy của Dijkstra và A*.

## Sử dụng chương trình dòng lệnh

Chương trình đọc dữ liệu rồi hỏi lần lượt điểm đầu, điểm cuối và thuật toán:

- Nhập ID nút, ví dụ `75617751`; hoặc
- Nhập tọa độ theo thứ tự `vĩ độ, kinh độ`, ví dụ `21.0285, 105.8522`. Chương trình chọn nút gần tọa độ đó nhất.

Lựa chọn thuật toán:

- `1`: chạy Dijkstra.
- `2`: chạy A*.
- `3`: chạy cả hai để so sánh.

Với lựa chọn `1` hoặc `2`, nếu tìm thấy đường đi, chương trình ghi đường đi vào `route.csv`, gồm ID nút, vĩ độ và kinh độ. Lựa chọn `3` chỉ in thống kê so sánh, không ghi `route.csv`.

## Dữ liệu đầu vào

### `nodes.csv`

Mỗi dòng là một nút trên bản đồ. File cần có các cột:

| Cột | Ý nghĩa |
| --- | --- |
| `osmid` | ID duy nhất của nút |
| `y` | Vĩ độ |
| `x` | Kinh độ |

Ví dụ:

```csv
osmid,y,x
75617751,21.0254887,105.8532462
```

### `edges.csv`

Mỗi dòng là một cạnh có hướng từ nút `u` đến nút `v`:

| Cột | Ý nghĩa |
| --- | --- |
| `u` | ID nút đầu cạnh |
| `v` | ID nút cuối cạnh |
| `key` | Trường bổ sung; chương trình hiện không dùng |
| `length` | Độ dài cạnh, tính bằng mét |

Ví dụ:

```csv
u,v,key,length
75617751,8184606185,0,66.28234166329261
```

Đồ thị là đồ thị có hướng. Nếu đường đi được phép đi theo cả hai chiều, dữ liệu cần có một dòng cho `u → v` và một dòng riêng cho `v → u`. Giao diện dùng cặp cạnh này để hiển thị hai mũi tên ngược chiều; nếu chỉ có một trong hai cạnh thì hiển thị một mũi tên. Mỗi ID ở `edges.csv` cần tồn tại trong `nodes.csv`.

Trình đọc CSV hiện hỗ trợ dữ liệu dạng số phân tách bằng dấu phẩy, không xử lý trường CSV có dấu phẩy nằm bên trong dấu nháy.

## Thuật toán

- **Dijkstra** tìm đường có tổng `length` nhỏ nhất; phù hợp với trọng số cạnh không âm.
- **A*** dùng khoảng cách đường chim bay Haversine giữa nút hiện tại và đích làm heuristic. Khoảng cách này được tính từ tọa độ vĩ độ, kinh độ và dùng làm ước lượng hướng tới đích.
- Cả hai thuật toán báo độ dài đường đi theo mét. Nếu không có đường nối từ điểm đầu tới điểm cuối theo hướng các cạnh, chương trình báo không tìm thấy đường.

## Cấu trúc file chính

| File | Vai trò |
| --- | --- |
| `Graph.java` | Đọc CSV, lưu đồ thị và chạy Dijkstra/A* |
| `Node.java`, `Edge.java` | Mô hình nút và cạnh |
| `Main.java` | Giao diện dòng lệnh |
| `MapApp.java` | Lớp khởi chạy giao diện JavaFX |
| `MapWindow.java` | Cửa sổ bản đồ, thao tác chuột và hiển thị kết quả |
| `pom.xml` | Cấu hình Maven, JDK mục tiêu và dependency JavaFX |
| `nodes.csv`, `edges.csv` | Dữ liệu nút và cạnh mẫu |
| `route.csv` | Đường đi được xuất ra bởi chương trình dòng lệnh |

## Xử lý lỗi thường gặp

### `package javafx... does not exist`

IntelliJ chưa nạp dependency từ Maven. Mở cửa sổ Maven, bấm **Reload All Maven Projects**, rồi build lại. Kiểm tra project SDK đang dùng JDK 11 trở lên và máy có thể truy cập Maven Central.

### `JavaFX runtime components are missing`

Chạy giao diện qua **Maven → Plugins → javafx → javafx:run**, hoặc chạy cấu hình **MapApp**. Không chọn `MapWindow` làm lớp chạy chính; `MapWindow` là lớp kế thừa JavaFX `Application`, còn `MapApp` là lớp khởi chạy.

### Không tìm thấy `nodes.csv` hoặc `edges.csv`

Đặt **Working directory** của cấu hình chạy thành thư mục project, hoặc truyền đường dẫn chính xác đến hai file làm tham số chương trình.

### Maven không tải được dependency

Kiểm tra kết nối mạng, proxy hoặc firewall rồi bấm **Reload All Maven Projects** lại. Các dependency JavaFX cần được tải từ Maven Central trong lần đầu.
