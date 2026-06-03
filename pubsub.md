# 1. Exchange có vai trò gì trong RabbitMQ?

Exchange là bộ định tuyến message. Producer không gửi trực tiếp vào queue, mà gửi vào exchange. Exchange quyết định message sẽ đi tới queue nào, nhiều queue hay bị bỏ qua.

---

# 2. Vì sao cần exchange, không gửi thẳng vào queue luôn?

Vì gửi thẳng vào queue làm producer phụ thuộc vào tên queue cụ thể. Có exchange thì producer chỉ cần biết “gửi vào đâu”, còn việc message đi tới queue nào do RabbitMQ quyết định qua binding và loại exchange.

---

# 3. Publish/Subscribe là gì?

Chapter này thuộc mô hình gì?
Đây là mô hình publish/subscribe. Một message được publish vào exchange, sau đó exchange copy message sang nhiều queue đã bind vào nó. Vì vậy nhiều consumer có thể cùng nhận một sự kiện.

---

# 4. Fanout exchange là gì?

Fanout exchange broadcast toàn bộ message nó nhận tới tất cả queue đang bind với nó. Nó không quan tâm routing key.

Khi nào dùng fanout?
Khi một event cần được phát cho nhiều bên cùng lúc, ví dụ log, notification, broadcast event, audit, metrics.

Khi nào không nên dùng fanout?
Khi cần phân loại message theo điều kiện, ví dụ chỉ gửi error log cho error queue, chỉ gửi payment event cho payment service. Lúc đó thường dùng direct hoặc topic.

---

# 5. Binding là gì?

Binding là mối liên kết giữa exchange và queue. Không có binding thì exchange không biết phải gửi message tới queue nào.

Binding quan trọng thế nào?
Rất quan trọng. Với fanout exchange, binding gần như là “đăng ký nhận message từ exchange này”. Queue nào bind thì queue đó sẽ nhận bản copy của message.

---

# 6. Temporary queue là gì?

Là consumer chỉ cần nhận log hiện tại, không cần lịch sử cũ. Queue tạm giúp tạo một queue mới, rỗng, riêng cho consumer đó.

Queue tạm khác queue thường ở điểm nào?
Queue tạm thường:

* có tên do server tự sinh
* không durable
* exclusive
* auto-delete

Khi nào dùng queue tạm?
Khi consumer chỉ cần dữ liệu đang “live” tại thời điểm nó kết nối, ví dụ:

* log viewer realtime
* dashboard tạm
* debug tool
* một subscriber ngắn hạn

Khi nào không dùng queue tạm?
Khi cần lưu task lâu dài, cần consumer dùng chung queue, hoặc cần message chờ xử lý bền vững như work queue.

---

# 7. Sao không dùng một queue chung cho tất cả log consumer?

Vì nếu dùng chung một queue thì các consumer sẽ chia tải theo kiểu work queue, chứ không phải mỗi consumer nhận một bản copy. 
Publish/subscribe cần mỗi consumer có queue riêng để tất cả đều nhận cùng một message.

---

# 8. Tại sao `queueDeclare()` không tham số lại tạo queue tạm?

RabbitMQ tự tạo cho ta một queue ngẫu nhiên. Queue này là queue tạm, dùng riêng cho consumer hiện tại và sẽ bị xóa khi connection đóng.

---

# 9. Routing key có vai trò gì trong fanout?

Với fanout exchange, routing key bị bỏ qua. Message được broadcast cho mọi queue đã bind.

Vậy routing key dùng khi nào?
Khi dùng direct hoặc topic exchange, routing key trở thành điều kiện định tuyến rất quan trọng.

---

# 10. Luồng hoạt động của publish/subscribe diễn ra thế nào?

Producer:

1. kết nối RabbitMQ
2. declare fanout exchange
3. publish message vào exchange

Consumer:

1. kết nối RabbitMQ
2. declare cùng exchange
3. tạo queue tạm
4. bind queue đó vào exchange
5. consume từ queue

RabbitMQ:

* copy message từ exchange sang mọi queue đã bind
* mỗi consumer đọc từ queue riêng của mình

---

# 11. Bài học chính của chapter Publish/Subscribe là gì?

* Producer không gửi trực tiếp vào queue
* Exchange là router
* Binding là luật nối exchange với queue
* Fanout exchange broadcast message cho tất cả queue bind vào nó
* Temporary queue phù hợp cho consumer ngắn hạn, realtime, không cần lưu lịch sử
* Mỗi consumer có queue riêng để nhận cùng một message

---

# 12. Ghi ngắn gọn chapter này như thế nào?

**Producer → Exchange → Bindings → Multiple Queues → Multiple Consumers**
Trong fanout: **một message được nhân bản sang tất cả queue đã bind**.
Temporary queue dùng khi consumer chỉ cần dữ liệu sống, không cần lưu lâu.
