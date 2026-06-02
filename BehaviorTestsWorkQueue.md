Behavior tests để thấy vì sao RabbitMQ cần ACK, QoS, Durable.

# Test 1 - Một worker, một task

Terminal 1:

```bash
java -cp ".;lib/*" Worker
```

Terminal 2:

```bash
java -cp ".;lib/*" NewTask Hello...
```

Expected:

Worker:

```text
[*] Waiting for messages...
[x] Received 'Hello...'
(đợi 3 giây)
[x] Done
```

---

# Test 2 - Queue hoạt động thật không?

Tắt worker.

Terminal 1:

```bash
java -cp ".;lib/*" NewTask Task1
java -cp ".;lib/*" NewTask Task2
java -cp ".;lib/*" NewTask Task3
```

Lúc này:

```text
Producer gửi
Worker chưa chạy
```

Sau đó mới chạy Worker:

```bash
java -cp ".;lib/*" Worker
```

Expected:

```text
[x] Received 'Task1'
[x] Done

[x] Received 'Task2'
[x] Done

[x] Received 'Task3'
[x] Done
```

=> Chứng minh RabbitMQ thật sự lưu queue.

---

# Test 3 - ACK hoạt động không?

Worker đang chạy.

Gửi task dài:

```bash
java -cp ".;lib/*" NewTask ..........
```

(10 dấu chấm = 10 giây)

Khi Worker in:

```text
[x] Received '..........'
```

ngay lập tức:

```text
Ctrl + C
```

kill Worker.

---

Vì:

```java
autoAck = false
```

và:

```java
basicAck(...)
```

chưa được gọi.

Message đang ở trạng thái:

```text
UNACK
```

---

Bây giờ chạy Worker lại:

```bash
java -cp ".;lib/*" Worker
```

Expected:

```text
[x] Received '..........'
```

xuất hiện lại.

=> Chứng minh ACK hoạt động.

---

# Test 4 - Fair Dispatch

Mở 2 terminal.

Terminal A:

```bash
java -cp ".;lib/*" Worker
```

Terminal B:

```bash
java -cp ".;lib/*" Worker
```

Bây giờ có:

```text
Worker1
Worker2
```

---

Gửi nhiều task:

```bash
java -cp ".;lib/*" NewTask .
java -cp ".;lib/*" NewTask ..
java -cp ".;lib/*" NewTask ...
java -cp ".;lib/*" NewTask ....
java -cp ".;lib/*" NewTask .....
```

Quan sát output.

Bạn sẽ thấy đại loại:

Worker1:

```text
Received '.'
Done

Received '...'
Done
```

Worker2:

```text
Received '..'
Done

Received '....'
Done
```

---

# Test 5 - Chứng minh basicQos(1)

Chạy 2 worker.

Worker1:

```text
Terminal A
```

Worker2:

```text
Terminal B
```

Gửi:

```bash
java -cp ".;lib/*" NewTask ..........
java -cp ".;lib/*" NewTask .
java -cp ".;lib/*" NewTask .
java -cp ".;lib/*" NewTask .
```

---

Điều cần quan sát:

Worker1 nhận:

```text
..........
```

và bận 10 giây.

Trong lúc đó Worker2 sẽ nhận liên tục:

```text
.
.
.
```

vì:

```java
channel.basicQos(1);
```

làm RabbitMQ hiểu:

```text
Worker1 đang giữ 1 UNACK
=> không giao thêm
```

---

Nếu bỏ:

```java
channel.basicQos(1);
```

và compile lại.

RabbitMQ sẽ quay về Round Robin.

Kết quả sẽ khác.

Đây là cách dễ nhất để tự thấy Fair Dispatch.

---

# Test 6 - Durability

Bạn đang dùng:

```java
queueDeclare(
    TASK_QUEUE_NAME,
    true,
    ...
)
```

và:

```java
MessageProperties.PERSISTENT_TEXT_PLAIN
```

---

Gửi vài task:

```bash
java -cp ".;lib/*" NewTask TaskA
java -cp ".;lib/*" NewTask TaskB
```

Đừng chạy Worker.

---

Dừng RabbitMQ:

```bash
docker stop rabbitmq
```

---

Khởi động lại:

```bash
docker start rabbitmq
```

---

Chạy Worker:

```bash
java -cp ".;lib/*" Worker
```

Nếu vẫn nhận:

```text
TaskA
TaskB
```

thì bạn đã chứng minh được:

```text
Durable Queue
+
Persistent Message
```

đang hoạt động.
