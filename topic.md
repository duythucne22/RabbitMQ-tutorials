# 1. Topic Exchange giải quyết vấn đề gì?

Fanout:
 - Gửi cho tất cả

Direct:
- Gửi cho queue có key khớp chính xác

Topic:
- Gửi theo pattern (mẫu)

---

Ví dụ:

```text
payment.created
payment.failed
payment.refunded

order.created
order.cancelled
order.shipped
```

Nếu dùng Direct Exchange:

```text
payment.created
payment.failed
payment.refunded
```

phải bind từng key.

---

Nếu dùng Topic Exchange:

`payment.*`

là đủ.

---

# 2. Topic Exchange khác Direct Exchange thế nào?

Direct:outingKey == bindingKey``
khớp tuyệt đối.

Ví dụ:

payment.failed
bindingKey = payment.failed`

→ match

---

payment.failed
bindingKey = payment.*`

→ không match

---

Topic:

`routingKey match pattern`

Ví dụ:

```text
payment.failed
bindingKey = payment.*`
```

→ match

---

# 3. Routing Key trong Topic Exchange là gì?

Là chuỗi nhiều phần được ngăn cách bởi dấu chấm.

Ví dụ:

```text
payment.created
payment.failed

order.created
order.shipped

user.deleted
```

---

Thường đặt theo:

```text
domain.action
```

hoặc

```text
domain.subdomain.action
```

---

Ví dụ:

```text
payment.card.failed
payment.bank.failed

order.shipping.created
order.shipping.completed
```

---

# 4. Vì sao Routing Key phải có cấu trúc?

Vì Topic Exchange route theo từng word.

Ví dụ:

```text
payment.failed
```

RabbitMQ hiểu:

```text
payment
failed
```

là 2 word.

---

Nhờ vậy mới hỗ trợ wildcard.

---

# 5. Dấu * có ý nghĩa gì?

```text
*
=
đúng 1 word
```

---

Ví dụ:

```text
payment.*
```

match:

```text
payment.created
payment.failed
payment.refunded
```

---

Không match:

```text
payment.card.failed
```

vì có 2 word phía sau.

---

# 6. Dấu # có ý nghĩa gì?

```text
#
=
0 hoặc nhiều word
```

---

Ví dụ:

```text
payment.#
```

match:

```text
payment.created

payment.card.failed

payment.bank.refunded

payment
```

---

Tất cả đều match.

---

# 7. Khi nào dùng * ?

Khi biết chính xác số tầng.

Ví dụ:

```text
payment.*
```

chỉ muốn:

```text
payment.created
payment.failed
payment.refunded
```

---

Không muốn:

```text
payment.card.failed
```

---

# 8. Khi nào dùng # ?

Khi muốn bắt toàn bộ nhánh.

Ví dụ:

```text
payment.#
```

---

Nhận:

```text
payment.created

payment.card.failed

payment.bank.failed

payment.bank.credit.refunded
```

---

# 9. Topic Exchange có thể giả lập Direct Exchange không?

Có.

Không dùng:

```text
*
#
```

---

Ví dụ:

`bindingKey = payment.failed`

Thì hoạt động y hệt:

Direct Exchange

---

# 10. Topic Exchange có thể giả lập Fanout Exchange không?

Có.

Bind:

```text
#
```

---

Nghĩa là:

```text
nhận mọi routing key
```

---

Hoạt động giống:

```text
Fanout Exchange
```

---

# 11. Một Queue có thể bind nhiều pattern không?

Có.

Ví dụ:

```text
payment.*
*.failed
```

---

Queue sẽ nhận:

```text
payment.created
payment.failed
order.failed
shipping.failed
```

---

# 12. Một message match nhiều binding thì sao?

RabbitMQ chỉ deliver:

```text
1 lần / queue
```

---

Ví dụ:

Queue bind:

```text
payment.*
*.failed
```

---

Message:

```text
payment.failed
```

match cả hai.

---

Kết quả:

```text
Queue nhận 1 message
```

không phải 2.

---

# 13. Topic Exchange thường dùng trong thực tế khi nào?

- Microservice Event Bus.

Ví dụ:

```text
order.created
order.shipped

payment.created
payment.failed

user.created
user.deleted
```

- Analytics Service:

```text
#
```

- Payment Service:

```text
payment.*
```

---

Alert Service:

```text
*.failed
```

---

Order Service:

```text
order.#
```

---

# 14. Topic Exchange mạnh hơn Direct Exchange ở đâu?

Direct:

```text
payment.created
payment.failed
payment.refunded
```

phải bind từng key.

---

Topic:

```text
payment.*
```

---

Sau này thêm:

```text
payment.cancelled
payment.completed
```

Không cần sửa binding.

---

# 15. Flow của Topic Exchange diễn ra thế nào?

Producer:

```text
basicPublish(
    exchange,
    "payment.failed",
    ...
)
```
    ↓
Exchange đọc:
- `routingKey = payment.failed`
    ↓
So sánh với tất cả binding:

```text
payment.*
*.failed
order.#
```
    ↓
Match:

```text
payment.*
*.failed
```
    ↓
Copy message tới các queue tương ứng.

---

# 16. Nên đặt tên Routing Key thế nào?

Khuyến nghị:

`domain.action`

Ví dụ:

```text
payment.created
payment.failed

order.created
order.shipped

user.deleted
```

Hoặc:

`domain.subdomain.action`

Ví dụ:

```text
payment.card.failed
payment.bank.failed

order.shipping.created
order.shipping.completed
```

---

# 17. Bài học chính của chapter Topic Exchange là gì?

Fanout:
- broadcast tất cả

Direct:
- route theo key chính xác

Topic:
- route theo pattern

Topic Exchange là Exchange linh hoạt nhất vì:

```text
Direct ⊂ Topic
Fanout ⊂ Topic
```

Nó cho phép thiết kế Event Bus theo cấu trúc tên:

```text
payment.*
order.#
*.failed
```

thay vì phải bind từng routing key riêng lẻ.
