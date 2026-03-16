# SWP391 Backend

Cấu trúc thư mục đã được sắp lại theo dạng:

- `.idea/`
- `swp391/`
- `.gitignore`
- `README.md`

Mã nguồn chính nằm trong:

`swp391/src/main/...`

Cách chạy project:

```bash
cd swp391
mvn spring-boot:run
```

Lưu ý:
- Toàn bộ file Maven như `pom.xml`, `mvnw`, `mvnw.cmd`, `.env`, `src` đã được đặt trong thư mục `swp391`.
- Thư mục build tạm `target` và metadata Git nội bộ `.git` không được đóng gói lại để repo gọn hơn.
