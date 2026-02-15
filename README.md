Tôi sẽ đưa cho bạn:

1. ✅ **Flow hoàn chỉnh chuẩn TCP application protocol**
2. 🔴 Danh sách bug cần fix (theo mức độ nghiêm trọng)
3. 🏗 Kiến trúc server nên refactor
4. 📡 Flow upload/download chuẩn byte-framing
5. 🔐 Các fix bảo mật bắt buộc

Tôi phân tích theo hướng: **thiết kế đúng giao thức + không corrupt stream + production mindset**

---

# I. Flow hoàn chỉnh đề xuất (Protocol chuẩn)

Hiện tại bạn đang trộn:

```
writeUTF()
+
read raw byte đến EOF
```

→ Sai vì TCP không có message boundary.

---

## 🎯 Thiết kế protocol chuẩn (Message-based)

### 1️⃣ Kết nối

Server → Client

```
WELCOME
```

---

### 2️⃣ LOGIN

Client:

```
LOGIN username password
```

Server:

```
OK
```

hoặc

```
ERROR message
```

Server phải lưu:

```java
private User currentUser;
```

---

### 3️⃣ LS

Client:

```
LS
```

Server:

```
OK
[fileCount]
file1.txt
file2.txt
...
```

---

### 4️⃣ UPLOAD

Client:

```
UPLOAD filename filesize
```

Server:

```
READY
```

Client:

```
<exactly filesize bytes>
```

Server:

```
OK
```

---

### 5️⃣ DOWNLOAD

Client:

```
DOWNLOAD filename
```

Server:

```
OK filesize
```

Client:

```
READY
```

Server:

```
<exactly filesize bytes>
```

Server:

```
DONE
```

---

# II. Flow thực thi bên trong ServerThread

```
while (connected) {

    readUTF() → command

    parse command

    switch(command) {

        LOGIN:
            validate
            set currentUser

        LS:
            list file

        UPLOAD:
            read fileSize
            read exactly fileSize bytes

        DOWNLOAD:
            write fileSize
            write exactly fileSize bytes

        EXIT:
            break

    }
}
```

---

# III. BUG CẦN FIX NGAY

## 🔴 1. Dùng readLine() thay vì readUTF()

Hiện tại:

```java
while ((line = dis.readLine()) != null)
```

❌ Sai hoàn toàn vì client dùng writeUTF()

✔ Fix:

```java
while (true) {
    String line = dis.readUTF();
```

---

## 🔴 2. Upload đọc đến EOF

Hiện tại:

```java
while ((bytesRead = dis.read(buffer)) != -1)
```

❌ Sẽ block vĩnh viễn

✔ Fix chuẩn:

```java
long remaining = fileSize;
while (remaining > 0) {
    int read = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining));
    if (read == -1) throw new IOException("Stream closed");
    out.write(buffer, 0, read);
    remaining -= read;
}
```

---

## 🔴 3. Không chống Path Traversal

Hiện tại:

```java
new File(defaultFolder, fileName);
```

Nếu client gửi:

```
../../windows/system32/a.txt
```

Bạn bị escape thư mục.

✔ Fix:

```java
File file = new File(defaultFolder, fileName);
if (!file.getCanonicalPath().startsWith(defaultFolder.getCanonicalPath())) {
    dos.writeUTF("ERROR invalid path");
    continue;
}
```

---

## 🔴 4. Không kiểm tra parts.length

Ví dụ:

```java
String parts[] = line.split("\\s+", 2);
```

Nếu client gửi:

```
UPLOAD
```

parts[1] sẽ lỗi.

✔ Fix:

```java
if (parts.length < 2) {
    dos.writeUTF("ERROR invalid command format");
    continue;
}
```

---

## 🟠 5. Không kiểm soát session

Login xong không lưu state:

```java
private User currentUser;
```

Trước mọi command:

```java
if (currentUser == null) {
    dos.writeUTF("ERROR login required");
    continue;
}
```

---

## 🟡 6. Flush quá nhiều

Client upload:

```java
dos.flush();
```

Flush mỗi 100KB là không cần thiết.

✔ Chỉ flush khi xong file.

---

## 🟡 7. Không xử lý NumberFormatException

```java
Long.parseLong(params[1])
```

Phải try-catch.

---

## 🟡 8. Không xử lý fileSize > limit

Nên giới hạn:

```java
if (fileSize > 100MB)
```

---

# IV. Flow Upload hoàn chỉnh (Chuẩn TCP)

### Client

```
writeUTF("UPLOAD file.txt 1024")
readUTF() → READY
write 1024 bytes
flush
readUTF() → OK
```

---

### Server

```
readUTF()
parse size
writeUTF("READY")
read exactly size bytes
writeUTF("OK")
```

---

# V. Flow Download hoàn chỉnh

### Client

```
writeUTF("DOWNLOAD file.txt")
readUTF() → OK 1024
writeUTF("READY")
read exactly 1024 bytes
```

---

### Server

```
readUTF()
writeUTF("OK 1024")
wait READY
write 1024 bytes
```

---

# VI. Kiến trúc Thread nên sửa

Hiện tại:

```java
extends Thread
```

Nên đổi sang:

```java
ExecutorService pool = Executors.newFixedThreadPool(10);
pool.submit(new ServerTask(socket));
```

Giúp:

* Giới hạn số thread
* Không crash khi nhiều client

---

# VII. Kiến trúc tổng thể nên hướng tới

```
Socket
   ↓
Protocol Parser
   ↓
Command Handler
   ↓
Service Layer
   ↓
DAO
```

Tách riêng:

* Parser
* Command
* Business logic

Đừng nhét tất cả vào run().

---

# VIII. Tổng hợp bug theo mức độ

### 🔴 Critical (phải fix trước)

* readLine() vs writeUTF mismatch
* Upload read đến EOF
* Không limit theo fileSize
* Không chống path traversal

---

### 🟠 Medium

* Không session
* Không validate input
* Không giới hạn file size

---

### 🟡 Minor

* Flush quá nhiều
* Không thread pool
* Không handle exception rõ ràng

---

# IX. Kết luận chuyên môn

Hiện tại code của bạn:

* 60% đúng về ý tưởng
* 40% sai ở tầng protocol

Vấn đề lớn nhất không phải Java syntax.

Mà là:

> TCP không có message boundary.
> Bạn phải tự thiết kế framing protocol.

---

Nếu bạn muốn, tôi có thể:

* Viết lại toàn bộ upload/download chuẩn production-level
* Hoặc vẽ state machine của server để bạn hiểu sâu cơ chế socket server hoạt động ra sao
