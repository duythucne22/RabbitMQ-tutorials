# 1. RPC trong RabbitMQ là gì?

RPC (Remote Procedure Call) là cách làm cho việc gọi một service từ xa trông giống như gọi một hàm local.

Ví dụ:

```java
String result = fibonacciRpc.call("10");
```

Nhìn giống:

```java
String result = fib(10);
```

nhưng thực tế:

```text
Client
   |
   | Request
   v
RabbitMQ
   |
   v
RPC Server
   |
   | Calculate
   v
RabbitMQ
   |
   | Response
   v
Client
```

---

# 2. Vì sao RabbitMQ cần RPC khi đã có Queue?

Work Queue chỉ phù hợp:
- Gửi task 
- Không cần trả kết quả

Ví dụ:
- Resize Image
- Send Email
- Generate Report

RPC phù hợp:
- Gửi request
- Đợi kết quả trả về

Ví dụ:
- Calculate Tax
- Get Exchange Rate
- Generate OTP
- Check User Exists

---

# 3. RPC khác Work Queue ở điểm nào?

Work Queue:

```text
Producer
   |
   v
Queue
   |
   v
Worker
```

Xử lý xong là hết.

RPC:

```text
Client
   |
   | Request
   v
rpc_queue
   |
   v
Server
   |
   | Response
   v
callback_queue
   |
   v
Client
```

Có chiều đi và chiều về.

---

# 4. Callback Queue là gì?

Là queue dùng để nhận kết quả trả về.

Client gửi request:

```java
fib(10)
```

nhưng server cần biết:
- Trả kết quả về đâu?

nên client tạo queue:

```java
String callbackQueue =
    channel.queueDeclare().getQueue();
```

Ví dụ:

```text
amq.gen.abc123
```

rồi gửi kèm:

```java
replyTo(callbackQueue)
```

---

# 5. replyTo dùng để làm gì?

```java
props = new BasicProperties
    .Builder()
    .replyTo(callbackQueue)
    .build();
```

Nó nói với server:
Sau khi xử lý xong, hãy publish kết quả vào queue này

Ví dụ:

```text
replyTo = amq.gen.abc123
```

---

# 6. correlationId là gì?

Mỗi request được gắn id duy nhất.

Ví dụ:

```java
UUID.randomUUID()
```

sinh:

A123

Client gửi:

```text
Request:
fib(10)

correlationId=A123
```

---

Server trả:

```text
Response:
55

correlationId=A123
```

Client nhìn:

```text
A123
```

=> biết response này thuộc request nào.

---

# 7. Vì sao cần correlationId?

Giả sử client gửi:

```text
fib(10)
fib(20)
fib(30)
```

cùng lúc.

Tất cả response đều quay về:

```text
callback_queue
```

thì:

```text
55
6765
832040
```

không biết cái nào của request nào.

CorrelationId giải quyết việc đó.

---

# 8. Vì sao không tạo callback queue cho mỗi request?

Có thể làm.

```text
Request 1
 -> callback_queue_1

Request 2
 -> callback_queue_2

Request 3
 -> callback_queue_3
```

Nhưng tốn tài nguyên.

Nên thực tế:

```text
1 Client
    |
    +---- 1 callback queue
```

và dùng:
`correlationId`

để phân biệt response.

---

# 9. Client gửi request như thế nào?

Client publish vào:
- `rpc_queue`

```java
channel.basicPublish(
    "",
    "rpc_queue",
    props,
    body
);
```

Luồng:

```text
Client
   |
   v
rpc_queue
```

---

# 10. Server nhận request như thế nào?

Server consume:

```java
channel.basicConsume(
    "rpc_queue",
    ...
);
```

Khi có request:

```text
rpc_queue
    |
    v
Server
```

---

# 11. Server biết trả lời về đâu bằng cách nào?

Từ:

```java
delivery.getProperties().getReplyTo()
```

Ví dụ:

```text
replyTo=amq.gen.abc123
```

Server chỉ việc:

```java
channel.basicPublish(
    "",
    replyTo,
    ...
)
```

---

# 12. Server phải copy correlationId vì sao?

Request:

```text
correlationId=A123
```

Server trả:

```text
correlationId=A123
```

Client nhận được:

```text
Response
correlationId=A123
```

=> ghép đúng request.

Nếu không copy:

```text
Client không biết response thuộc ai
```

---

# 13. Tại sao server vẫn cần ACK?

RPC vẫn là consume message bình thường.

Nên:

```java
channel.basicAck(...)
```

vẫn cần.

Nếu server chết giữa chừng:

```text
Request chưa ACK
```

RabbitMQ sẽ:

```text
Requeue
```

và server khác có thể xử lý lại.

---

# 14. Vì sao RPC có thể nhận duplicate response?

Kịch bản:

```text
Client gửi request
```

Server:

```text
Tính xong
```

Server:

```text
Publish response
```

Nhưng trước khi:

```text
basicAck(request)
```

thì server chết.

RabbitMQ thấy:

```text
Request chưa ACK
```

=> gửi lại request.

Server mới xử lý lại.

Kết quả:

```text
Client nhận 2 response giống nhau
```

---

# 15. Vì sao RabbitMQ khuyên RPC phải idempotent?

Vì duplicate request là hoàn toàn có thể xảy ra.

Ví dụ tốt:

```text
fib(10)
```

chạy 2 lần:

```text
55
55
```

không sao.

---

Ví dụ xấu:

```text
Transfer $100
```

chạy 2 lần:

```text
-100
-100
```

=> mất tiền.

Nên RPC handler nên:

```text
Idempotent
```

hoặc có cơ chế chống xử lý trùng.

---

# 16. CompletableFuture trong client dùng để làm gì?

```java
CompletableFuture<String> response
```

Đóng vai trò:

```text
Chờ kết quả RPC
```

Callback:

```java
response.complete(...)
```

Khi response tới.

---

Sau đó:

```java
response.get()
```

sẽ block.

```text
Đợi tới khi server trả lời
```

---

# 17. Flow hoàn chỉnh của RPC

```text
Client

Create callback queue
Create correlationId
        |
        v
Publish Request
to rpc_queue
(replyTo + correlationId)
        |
        v
====================
RabbitMQ
====================
        |
        v
RPC Server

Receive Request

Process

fib(n)

Publish Response
to replyTo queue

Copy correlationId

ACK request
        |
        v
====================
RabbitMQ
====================
        |
        v
callback queue
        |
        v
Client

Check correlationId

Complete Future

Return result
```

---

# 18. Bài học chính của chapter RPC là gì?

* RPC = Request/Response qua RabbitMQ
* Request gửi vào `rpc_queue`
* Response gửi về `callback_queue`
* `replyTo` cho server biết trả lời ở đâu
* `correlationId` giúp ghép response với request
* Một client thường dùng một callback queue
* Server phải copy correlationId sang response
* ACK vẫn cần như Work Queue
* Duplicate response có thể xảy ra
* RPC handler nên idempotent
* RabbitMQ khuyến khích async pipeline hơn RPC nếu có thể thiết kế được hệ thống theo hướng bất đồng bộ.
