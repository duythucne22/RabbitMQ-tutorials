# 1. Connection là gì?

```java
Connection connection = factory.newConnection();
```

Đây là kết nối TCP thật giữa Java app và RabbitMQ Server.

Tưởng tượng:

```text
Send.java
    |
 TCP Socket
    |
RabbitMQ Server
```

Giống như MySQL:

```java
DriverManager.getConnection(...)
```

hay Redis:

```java
new Jedis(...)
```

Mỗi `Connection` khá đắt đỏ vì phải:

* mở socket
* authenticate
* handshake protocol

nên thường ít tạo.

---

# 2. Channel là gì?

```java
Channel channel = connection.createChannel();
```

- Đây KHÔNG phải queue.
- Đây cũng KHÔNG phải nơi lưu message.
- RabbitMQ dùng khái niệm:

```text
1 TCP Connection
        |
        +--- Channel 1
        +--- Channel 2
        +--- Channel 3
```

Một Connection có thể chứa nhiều Channel.

Ví dụ:

```text
Application
    |
    +---- Connection
              |
              +---- Channel A
              +---- Channel B
              +---- Channel C
```

RabbitMQ khuyến khích:

```text
Ít Connection
Nhiều Channel
```

vì Channel rẻ hơn nhiều.

---

## createChannel là tạo mới hay kết nối vào channel cũ?

Là tạo channel mới.

```java
connection.createChannel();
```

Mỗi lần gọi là RabbitMQ tạo một channel mới.

---

## Vậy Send và Recv khác channel sao vẫn nói chuyện được?

Đây là chỗ quan trọng nhất.
Bạn đang nghĩ:

```text
Send Channel
     |
     v
Recv Channel
```

Không phải.
Thực tế:

```text
Send Channel
     |
     v
Queue "hello"
     ^
     |
Recv Channel
```

Hai channel không cần giống nhau.
Chúng chỉ cần cùng thao tác trên:

```text
Queue hello
```
---

# 3. Map<String,Object> dùng làm gì?

```java
Map<String, Object> args =
    Map.of("x-queue-type", "quorum");
```

Đây là các option của queue.

Giống:

```java
new HashMap<>();
```

nhưng immutable.

Ở đây:

```java
"x-queue-type" -> "quorum"
```

nghĩa là:

```text
Tạo queue loại quorum
```

Nếu không truyền:

```java
Map.of()
```

thì queue dùng cấu hình mặc định.

---

# 4. queueDeclare làm gì?

```java
channel.queueDeclare(
    QUEUE_NAME,
    true,
    false,
    false,
    args
);
```

Nó không phải:

```text
Lấy queue
```

Nó là:

```text
Đảm bảo queue tồn tại
```

Nếu queue chưa có:

```text
RabbitMQ tạo queue
```

Nếu queue đã có:

```text
Không làm gì
```

Cho nên gọi nhiều lần vẫn ổn.

---

# 5. Vì sao Send và Recv đều queueDeclare?

Đây là ý đồ rất hay.
Giả sử:

```text
08:00 Recv chạy trước
08:05 Send mới chạy
```

Nếu Recv không declare:

```text
Queue chưa tồn tại
=> lỗi
```

Ngược lại:

```text
08:00 Send chạy trước
08:05 Recv chạy
```

Nếu Send không declare:

```text
Publish vào queue chưa tồn tại
=> lỗi
```

Nên cả hai đều:

```java
queueDeclare(...)
```

để đảm bảo queue luôn tồn tại.

---

# 6. basicPublish hoạt động thế nào?

```java
channel.basicPublish(
    "",
    QUEUE_NAME,
    null,
    message.getBytes()
);
```

Đây là lệnh gửi message.
nên RabbitMQ hiểu:

```text
Đưa message vào queue hello
```

Luồng:

```text
Send
 |
 | Hello World
 v
Queue hello
```

---

# 7. RabbitMQ lưu message ở đâu?

Khi bạn chạy:

```java
basicPublish(...)
```

message được lưu trong queue:

```text
Queue hello

+----------------+
| Hello World    |
+----------------+
```

Queue này nằm trong RabbitMQ Server.

Không nằm trong:

```text
Send.java
Recv.java
```

---

# 8. DeliverCallback là gì?

Đây là phần quan trọng nhất phía consumer.

```java
DeliverCallback deliverCallback =
    (consumerTag, delivery) -> {
        ...
    };
```

Nó là callback.
RabbitMQ sẽ gọi hàm này mỗi khi có message tới.
Tưởng tượng:

```text
Recv
 |
 | "Có message chưa?"
 | "Có message chưa?"
 | "Có message chưa?"
```

Không.
RabbitMQ chủ động push:

```text
Queue hello
     |
     v
DeliverCallback
```

---

# 9. delivery là gì?

Trong callback:

```java
(consumerTag, delivery)
```

thứ bạn cần nhất là:

```java
delivery
```

Nó chứa:

```text
Body
Headers
Metadata
Routing Info
...
```

Lấy body:

```java
delivery.getBody()
```

---

# 10. basicConsume làm gì?

```java
channel.basicConsume(
    QUEUE_NAME,
    true,
    deliverCallback,
    consumerTag -> {}
);
```

Đây là câu lệnh:

```text
"Tôi muốn subscribe queue hello"
```

Sau khi gọi:

```text
Recv
   |
   +---- đăng ký với queue hello
```

RabbitMQ ghi nhận:

```text
Consumer này đang nghe queue hello
```

Khi có message:

```text
Queue hello
      |
      v
DeliverCallback
```

RabbitMQ tự gọi callback.

---

# Execution flow của bài Hello World

```text
Send.java

Connection
    |
Channel
    |
queueDeclare("hello")
    |
basicPublish("Hello World")
    |
    v

=========================
 RabbitMQ Server
=========================

Queue hello

+----------------+
| Hello World    |
+----------------+

    |
    v

Recv.java

Connection
    |
Channel
    |
queueDeclare("hello")
    |
basicConsume(...)
    |
DeliverCallback
    |
println(...)
```
