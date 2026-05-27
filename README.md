# P2P FILE SHARING SYSTEM

Hệ thống chia sẻ file Peer-to-Peer (P2P) được xây dựng bằng Java, hỗ trợ upload/download file thông qua cơ chế chia nhỏ file thành nhiều chunk và tải song song từ nhiều peer khác nhau.

Hệ thống mô phỏng cơ chế hoạt động cơ bản của BitTorrent:

* Chunk Distribution
* Download song song nhiều peer
* Multithreading
* Verify MD5
* Retry khi peer lỗi
* Peer Discovery thông qua Tracker Server

# TÍNH NĂNG

* Upload file
* Chia file thành nhiều chunk
* Download từ nhiều peer khác nhau
* Download song song bằng multithreading
* Thuật toán rarest-first
* Chunk distribution
* Quản lý peer online
* Verify MD5 đảm bảo tính toàn vẹn dữ liệu
* Retry khi peer mất kết nối
* Merge file sau khi download
* GUI bằng Java Swing
* Hiển thị peer online
* Hiển thị chunk peer đang có
* Theo dõi log hệ thống và tốc độ download

---

# PHÂN CÔNG NHIỆM VỤ

| TT | Công việc / Nhiệm vụ                                                                      | SV thực hiện     | Đóng góp |
| -- | ----------------------------------------------------------------------------------------- | ---------------- | -------- |
| 1  | Xây dựng peer-node, common, tracker, socket programming, multithreading, download service | Phạm Quốc Anh    | 60%      |
| 2  | Xây dựng tracker, GUI, chunk distribution, verify MD5, retry mechanism                    | Nguyễn Đức Khánh | 40%      |

---

# CÔNG NGHỆ SỬ DỤNG

| Công nghệ            | Mục đích                       |
| -------------------- | ------------------------------ |
| Java                 | Ngôn ngữ lập trình chính       |
| Java Swing           | Xây dựng giao diện GUI         |
| TCP Socket           | Truyền dữ liệu giữa các peer   |
| HTTP/JSON            | Giao tiếp giữa Peer và Tracker |
| Jackson ObjectMapper | Serialize/Deserialize JSON     |
| Multithreading       | Download song song             |
| ExecutorService      | Quản lý thread pool            |
| ConcurrentHashMap    | Đồng bộ dữ liệu đa luồng       |
| MD5                  | Verify dữ liệu                 |
| Maven                | Quản lý project                |

---

# CÀI ĐẶT

## YÊU CẦU

* Java JDK 21
* Maven 3+
* IntelliJ IDEA hoặc VSCode

Kiểm tra version:

```bash
java -version
mvn -version
```

---

# BUILD PROJECT

Tại thư mục gốc project:

```bash
mvn clean install
```

Sau khi build:

* class files
* dependencies
* target

sẽ được tạo tự động.

---

# CHẠY HỆ THỐNG

## 1. Chạy Tracker Server

Mở terminal:

```bash
cd tracker
mvn spring-boot:run
```

Hoặc:

```bash
java -jar target/tracker.jar
```

Kết quả:

```text
Tracker Server started on port 8080
```

---

## 2. Chạy Peer Node

Mỗi peer chạy trên một port khác nhau.
truy cập cd peer-node

### Peer 1

```bash
java -cp "target/classes;target/dependency/*" com.p2p.ui.PeerUI
```

### Peer 2

```bash
java -cp "target/classes;target/dependency/*" com.p2p.ui.PeerUI
```

### Peer 3

```bash
java -cp "target/classes;target/dependency/*" com.p2p.ui.PeerUI
```

Sau khi chạy:

* GUI Peer UI sẽ xuất hiện
* Peer tự động register với Tracker

---

# HƯỚNG DẪN SỬ DỤNG

## 1. Upload File

* Chọn file trên giao diện
* Click "Upload/Register"

Hệ thống sẽ:

* chia file thành chunk
* tạo metadata
* tính hash MD5
* lưu chunk vào storage
* register với Tracker

---

## 2. Search File

* Nhập tên file
* Click "Search"

Hệ thống hiển thị:

* danh sách peer chứa file
* danh sách chunk peer đang có

---

## 3. Download Chunk

* Click vào peer
* Chọn chunk cần tải
* Click download

---

## 4. Download All

* Click "Download All Missing"

Hệ thống sẽ:

* lấy metadata
* lấy bitfield
* tải chunk song song
* verify MD5
* merge file hoàn chỉnh

---

# GIAO DIỆN HỆ THỐNG
<img width="1917" height="1005" alt="image" src="https://github.com/user-attachments/assets/168b7ed4-aac2-469a-8daf-740ebebdfc84" />


---

## Upload File

Cho phép:

* chọn file
* upload file
* register với Tracker
  <img width="1912" height="1017" alt="image" src="https://github.com/user-attachments/assets/8abb35da-4edd-40b8-a816-ed4568bb7310" />


---

## Search & Download

Hiển thị:

* peer chứa file
* chunk distribution
* download chunk
* download all

  <img width="1048" height="538" alt="image" src="https://github.com/user-attachments/assets/97b2e373-5b91-48d9-b5cd-cbdcf2c078e1" />


---

## System Logs

Hiển thị:

* heartbeat
* upload
* download
* verify MD5
* merge file
* retry peer

  <img width="1061" height="308" alt="image" src="https://github.com/user-attachments/assets/50c36d12-95f1-4bb0-b5e9-bb4fe4c8d280" />


---

# CẤU TRÚC PROJECT

```text
p2p-file-sharing/
│
├── common
│   ├── dto
│   ├── message
│   ├── model
│   └── util
│
├── tracker
│   ├── controller
│   ├── service
│   └── repository
│
├── peer-node
│   ├── network
│   ├── service
│   ├── storage
│   ├── tracker
│   └── ui
│
├── downloads
├── storage
└── pom.xml
```

---

# CÁC MESSAGE CHÍNH

| Message            | Chức năng           |
| ------------------ | ------------------- |
| REGISTER           | Đăng ký peer        |
| HEARTBEAT          | Cập nhật trạng thái |
| REQUEST_CHUNK      | Yêu cầu chunk       |
| SEND_CHUNK         | Gửi chunk           |
| BITFIELD           | Danh sách chunk     |
| HAVE               | Thông báo chunk mới |
| FILE_METADATA      | Metadata file       |
| CHUNK_DISTRIBUTION | Phân phối chunk     |

---

# KIẾN TRÚC HỆ THỐNG

Hệ thống được xây dựng theo mô hình P2P lai gồm:

* Tracker Server
* Peer Node
* Peer UI

Tracker chỉ:

* quản lý peer
* quản lý file
* hỗ trợ peer discovery

Dữ liệu file được truyền trực tiếp giữa các peer thông qua TCP Socket.

---

# TÍNH NĂNG NỔI BẬT

* Download từ nhiều peer
* Download song song nhiều chunk
* Rarest-first algorithm
* Retry khi peer lỗi
* Verify MD5
* Chunk distribution
* GUI trực quan
* Multithreading


