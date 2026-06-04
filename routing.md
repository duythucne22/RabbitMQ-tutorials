# 1. Direct Exchange là gì?

Direct Exchange định tuyến message dựa trên việc so khớp chính xác:

`routingKey == bindingKey`

Nếu khớp:

Message được gửi tới queue

Nếu không khớp:

Message bị bỏ qua

---

# 2. Direct Exchange giải quyết vấn đề gì?

Fanout:

Gửi cho tất cả queue

Direct:

Gửi cho đúng queue quan tâm

Ví dụ:
- error
- warning
- info

Mỗi queue chỉ nhận loại message nó đăng ký.

---

# 3. Binding Key là gì?

Binding Key là điều kiện mà queue dùng để đăng ký nhận message.

Ví dụ:

```java
queueBind(
    queueName,
    "logs",
    "error"
);
```

Nghĩa là:

Queue này muốn nhận message loại error

---

# 4. Routing Key là gì?

Routing Key là nhãn mà producer gắn vào message khi publish.

Ví dụ:

```java
basicPublish(
    "logs",
    "error",
    ...
);
```

Nghĩa là:

Message này thuộc loại error

---

# 5. Direct Exchange route message như thế nào?

RabbitMQ so sánh:
`routingKey` với `bindingKey`

Ví dụ:

`routingKey = error`

Queue A:
`bindingKey = error`

Queue B:
`bindingKey = warning`

Kết quả:

Queue A nhận
Queue B không nhận

---

# 6. Một Queue có thể bind nhiều lần không?

Có.

Ví dụ:

```java
queueBind(queue, exchange, "error");
queueBind(queue, exchange, "warning");
```

Queue đó sẽ nhận:

- error
- warning

---

# 7. Nhiều Queue có thể dùng chung Binding Key không?

Có.

Ví dụ:

Queue A -> error
Queue B -> error

Khi producer gửi:

routingKey = error

Cả hai queue đều nhận.

---

# 8. Khi nhiều queue cùng bind một key thì Direct Exchange hoạt động ra sao?

Lúc này nó gần giống fanout cho riêng nhóm đó.

Ví dụ:
Queue A -> error
Queue B -> error

Message:
- error

Kết quả:
- Queue A nhận
- Queue B nhận

---

# 9. `exchangeDeclare()` dùng để làm gì?

```java
channel.exchangeDeclare(
    "direct_logs",
    "direct"
);
```

Đảm bảo exchange tồn tại.

Nếu chưa có:

RabbitMQ tạo exchange

Nếu đã có:

Không làm gì

Tương tự:

```java
queueDeclare(...)
```

nhưng dành cho exchange.

---

# 10. `queueBind()` dùng để làm gì?

```java
queueBind(
    queue,
    exchange,
    "error"
);
```

Tạo binding giữa:

```textext
Exchange
    |
Binding Key
    |
Queue
```

Nghĩa là:

Queue đăng ký nhận message error

---

# 11. `basicPublish()` trong Direct Exchange hoạt động thế nào?

```java
basicPublish(
    exchange,
    routingKey,
    ...
);
```

Producer:

gắn routingKey cho message

Exchange:

dùng routingKey để tìm queue phù hợp

---

# 12. `basicConsume()` làm gì?


```java
basicConsume(...)
```

để đăng ký nhận message từ queue.

Điểm mới nằm ở:

Exchange
Routing Key
Binding Key


không phải consume.

---

# 13. Fanout và Direct khác nhau thế nào?

Fanout:

Không quan tâm key

```text
1 message
    ↓
Tất cả queue
```

---

Direct:

Quan tâm key

```text
1 message
    ↓
Chỉ queue match key
```

---

# 14. Khi nào dùng Direct Exchange?

Khi cần phân loại message.

Ví dụ:

Log Level

- info
- warning
- error

---

Ví dụ:

Payment Event

- payment.created
- payment.failed
- payment.refunded

---

Ví dụ:

Notification

- email
- sms
- push

---

# 15. Luồng hoạt động của Direct Exchange

```text
Producer

basicPublish(
    exchange,
    routingKey
)
        |
        v
Direct Exchange
        |
        | routingKey == bindingKey ?
        v
Matching Queue
        |
        v
Consumer
```

---

# 16. Bài học chính của chapter Routing

* Exchange có thể lọc message thay vì broadcast.
* Producer cung cấp `routingKey`.
* Queue đăng ký bằng `bindingKey`.
* Direct Exchange route theo:

`routingKey == bindingKey`

* Một queue có thể đăng ký nhiều loại message.
* Nhiều queue có thể cùng nhận một loại message.
* Routing giúp giảm việc broadcast không cần thiết và gửi đúng message tới đúng consumer.

---

# 17. Ghi ngắn gọn chapter này như thế nào?

```text
Producer
    |
routingKey
    |
Direct Exchange
    |
bindingKey
    |
Matching Queues
    |
Consumers
```

Quy tắc cốt lõi:

`routingKey == bindingKey`

thì:

Message được route tới queue.
