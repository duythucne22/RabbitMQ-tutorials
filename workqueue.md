# 1. Work Queue giải quyết bài toán gì?

Một Producer có thể tạo ra rất nhiều task.

```text
Producer
    |
    v
Task Queue
    |
    +-- Worker 1
    +-- Worker 2
    +-- Worker 3
```

Thay vì:

- 1 Producer
- 1 Consumer

ta có:

- 1 Producer
- N Consumer

để chia tải xử lý.

---

# 2. Nếu Producer gửi task khi chưa có Worker thì sao?

Task vẫn nằm trong Queue.

```text
Producer
    |
    v
Queue

Task A
Task B
Task C
```

Worker có thể xuất hiện sau.
RabbitMQ sẽ giữ task giúp ta.

=> **Queue đóng vai trò Buffer.**

---

# 3. Nếu Worker đang xử lý mà bị crash thì sao?

Nếu:

```java
autoAck = true
```

RabbitMQ coi như task hoàn thành ngay khi giao.

```text
Worker nhận task
      |
      +-- chết
```

=> Task mất.

---

Nếu:

```java
autoAck = false
```

và dùng:

```java
channel.basicAck(...)
```

thì:

```text
Worker nhận task
      |
      +-- chết trước ACK
```

=> RabbitMQ requeue task.
=> Task không mất.

---

# 4. ACK là gì?

ACK = Acknowledgement (tín hiệu)

```text
"Tôi xử lý xong rồi"
```

gửi từ Worker về RabbitMQ.

```text
Worker
   |
   | ACK
   v
RabbitMQ
```

Sau ACK:

```text
RabbitMQ được phép xóa message.
```

---

# 5. Message có những trạng thái nào?

Message đang nằm trong queue.
```text
READY
```
↓
Message đã giao cho Worker.
Worker chưa ACK.
```text
UNACK
```
↓
Worker xác nhận xử lý xong.
```text
ACKED
```
↓
RabbitMQ xóa message.
```text
DELETED
```

---

# 6. Nếu RabbitMQ restart thì sao?

ACK chỉ bảo vệ khi:

```text
Worker chết
```

không bảo vệ khi:

```text
RabbitMQ chết
```

---

Ví dụ:

```text
Queue

Task A
Task B
```

RabbitMQ restart.

Mặc định:

```text
Queue mất
Task mất
```

---

# 7. Durable Queue là gì?

```java
queueDeclare(
    "task_queue",
    true,
    ...
)
```

Ý nghĩa:
**Queue phải tồn tại sau khi RabbitMQ restart.**

---

Durable Queue bảo vệ:
**Queue Metadata**

không phải Message.

---

# 8. Durable Queue có đủ chưa?

Chưa.

Có thể xảy ra:
- Queue còn
- Task mất

Vì Message mặc định không persistent.

---

# 9. Persistent Message là gì?

```java
MessageProperties.PERSISTENT_TEXT_PLAIN
```

Ý nghĩa:
`RabbitMQ cố gắng ghi message xuống disk.`

Muốn tăng khả năng sống sót sau restart cần:
`Durable Queue + Persistent Message`

---

# 10. Persistence có đảm bảo 100% không?

Không.

Vẫn có cửa sổ nhỏ:

```text
RabbitMQ nhận message
       |
       +-- chưa flush disk
       |
       +-- crash
```

=> `Có thể mất.`

---

Nếu cần mạnh hơn:

`Publisher Confirm`

---

# 11. RabbitMQ phân phối task cho nhiều Worker thế nào?

Mặc định:

Round Robin

Ví dụ:

```text
Task1 -> Worker1
Task2 -> Worker2
Task3 -> Worker1
Task4 -> Worker2
```

RabbitMQ không quan tâm:

`Worker nào đang bận.`

---

# 12. Vấn đề của Round Robin là gì?

Ví dụ:

```text
Task1 = 10s
Task2 = 1s
Task3 = 10s
Task4 = 1s
```

Round Robin:

```text
Worker1 -> Task1 + Task3
Worker2 -> Task2 + Task4
```

Kết quả:

```text
Worker1 quá tải
Worker2 nhàn rỗi
```

---

# 13. Fair Dispatch là gì?

Ý tưởng:
`Task mới nên giao cho Worker đang rảnh.`

không phải:
`giao theo thứ tự cứng nhắc.`

---

# 14. basicQos(1) làm gì?

```java
channel.basicQos(1);
```

Ý nghĩa:
- Mỗi Worker chỉ được giữ tối đa
- 1 message chưa ACK.

---

Nếu Worker đang giữ:

`1 UNACK Message`

RabbitMQ sẽ:

`không giao thêm task.`

---

# 15. RabbitMQ biết Worker nào bận bằng cách nào?

RabbitMQ không biết:
- `Task mất bao lâu.`

RabbitMQ chỉ nhìn:
- `ACK hay chưa ACK.`

---

Trong mắt RabbitMQ:
- `ACK = Worker rảnh`
- `UNACK = Worker bận`

---

# 16. ACK và Fair Dispatch liên hệ thế nào?

Fair Dispatch phụ thuộc vào ACK.

Không có:

```java
autoAck = false
```

thì:
**mọi Worker luôn được coi là rảnh.**

---

Do đó:

```java
basicQos(1)
```

**gần như vô nghĩa.**

---

# Execution Flow của Work Queue

```text
Producer

NewTask
   |
   v
basicPublish()

=========================
 RabbitMQ
=========================

task_queue

READY
  |
  v
Worker

basicConsume()
  |
  v
UNACK
  |
  v
doWork()
  |
  v
basicAck()
  |
  v
Message Deleted
```
