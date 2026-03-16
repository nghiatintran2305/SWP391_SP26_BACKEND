# Huong dan cai dat SWP391 Backend

## Yeu cau he thong

- **Java**: JDK 17 hoac cao hon (khuyen nghi JDK 21)
- **PostgreSQL**: Version 12 hoac cao hon
- **Maven**: 3.6+ (hoac su dung Maven Wrapper co san)

## Buoc 1: Cai dat PostgreSQL

### 1.1. Tai va cai dat PostgreSQL
- Download tu: https://www.postgresql.org/download/
- Trong qua trinh cai dat, nho mat khau cua user `postgres`

### 1.2. Tao Database

Mo **pgAdmin** hoac **psql** va chay:

```sql
CREATE DATABASE "BE"
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;
```

### 1.3. Chay script khoi tao database

Mo file `database_setup.sql` va chay toan bo noi dung trong pgAdmin:

1. Mo pgAdmin
2. Ket noi vao database `BE`
3. Mo Query Tool (Tools > Query Tool)
4. Copy noi dung file `database_setup.sql` vao
5. Nhan F5 hoac click Execute

## Buoc 2: Cau hinh ung dung

### 2.1. Chinh sua file application.properties

Mo file `src/main/resources/application.properties` va cap nhat:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/BE
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD_HERE
```

Thay `YOUR_PASSWORD_HERE` bang mat khau PostgreSQL cua ban.

## Buoc 3: Chay ung dung

### 3.1. Su dung Maven Wrapper (khuyen nghi)

Mo terminal/command prompt tai thu muc `swp391`:

**Windows:**
```cmd
.\mvnw.cmd spring-boot:run
```

**Linux/Mac:**
```bash
./mvnw spring-boot:run
```

### 3.2. Hoac su dung IntelliJ IDEA

1. Mo project trong IntelliJ
2. Tim file `Swp391Application.java`
3. Click phai > Run 'Swp391Application'

## Buoc 4: Kiem tra

### 4.1. Truy cap Swagger UI

Mo trinh duyet va vao: http://localhost:8080/swagger-ui/index.html

### 4.2. Test API bang Postman

1. Import file `SWP391_Account_Management.postman_collection.json` vao Postman
2. Chay request "Login as Admin" truoc
3. Sau do test cac request khac

## Thong tin dang nhap

| Role | Username | Password | LoginType |
|------|----------|----------|-----------|
| Admin | admin1 | 123456 | ADMIN |
| Admin | admin2 | 123456 | ADMIN |
| Admin | admin3 | 123456 | ADMIN |
| Lecturer | lecturer1 | 123456 | USER |
| Lecturer | lecturer2 | 123456 | USER |
| Lecturer | lecturer3 | 123456 | USER |
| Student | student1 | 123456 | USER |
| Student | student2 | 123456 | USER |
| Student | student3 | 123456 | USER |

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - Dang nhap

### User Operations (Can dang nhap)
- `POST /api/v1/accounts/register/student` - Dang ky tai khoan student (Khong can dang nhap)
- `GET /api/v1/accounts/me` - Lay thong tin tai khoan hien tai
- `PUT /api/v1/accounts/me` - Cap nhat thong tin tai khoan
- `PUT /api/v1/accounts/me/change-password` - Doi mat khau
- `DELETE /api/v1/accounts/me` - Xoa tai khoan

### Admin Operations (Can role ADMIN)
- `GET /api/v1/accounts` - Lay danh sach tat ca tai khoan
- `GET /api/v1/accounts/{id}` - Lay thong tin tai khoan theo ID
- `GET /api/v1/accounts/lecturers` - Lay danh sach lecturers
- `GET /api/v1/accounts/students` - Lay danh sach students
- `POST /api/v1/accounts/lecturers` - Tao tai khoan lecturer moi
- `PUT /api/v1/accounts/{id}` - Cap nhat tai khoan bat ky
- `DELETE /api/v1/accounts/{id}` - Xoa tai khoan

## Xu ly loi thuong gap

### Loi: Port 8080 da duoc su dung
```
Web server failed to start. Port 8080 was already in use.
```
**Giai phap:** Tat ung dung dang chay tren port 8080, hoac doi port trong application.properties:
```properties
server.port=8081
```

### Loi: Khong ket noi duoc database
```
Connection refused
```
**Giai phap:** Kiem tra PostgreSQL da chay chua, va thong tin ket noi trong application.properties dung chua.

### Loi: Role khong ton tai
```
Role STUDENT khong ton tai
```
**Giai phap:** Chay lai script `database_setup.sql` de tao roles.
