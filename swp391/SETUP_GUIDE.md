# Huong dan cai dat SWP391 Backend

## Yeu cau he thong

- **Java**: JDK 17 hoac cao hon
- **PostgreSQL**: Version 12 hoac cao hon
- **Maven**: 3.6+ hoac Maven Wrapper

## Buoc 1: Cai dat PostgreSQL

### 1.1. Tao database

```sql
CREATE DATABASE "BE"
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;
```

## Buoc 2: Tao file cau hinh `.env`

Trong thu muc `swp391`, copy file mau:

**Windows**
```cmd
copy .env.example .env
```

**Linux/Mac**
```bash
cp .env.example .env
```

Sau do mo `.env` va dien cac gia tri that cua ban.

### 2.1. Database

```properties
POSTGRES_URL=jdbc:postgresql://localhost:5432/BE
POSTGRES_USER=postgres
POSTGRES_PASSWORD=YOUR_POSTGRES_PASSWORD
```

### 2.2. JWT

```properties
JWT_SECRET=YOUR_32_CHARACTERS_OR_LONGER_SECRET
JWT_EXPIRATION=86400000
```

### 2.3. Gmail SMTP de gui OTP

Mau cau hinh:

```properties
OTP_EXPIRATION_MINUTES=10
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
SPRING_MAIL_USERNAME=your-gmail-address@gmail.com
SPRING_MAIL_PASSWORD=your-16-char-gmail-app-password
MAIL_FROM=your-gmail-address@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
```

Luu y:

1. Bat `2-Step Verification` cho tai khoan Gmail.
2. Tao `App Password` trong Google Account.
3. Dung `App Password` cho `SPRING_MAIL_PASSWORD`.
4. Khong dung mat khau Gmail thuong.
5. `MAIL_FROM` nen trung voi email gui.

## Buoc 3: Chay ung dung

**Windows**
```cmd
.\mvnw.cmd spring-boot:run
```

**Linux/Mac**
```bash
./mvnw spring-boot:run
```

## Buoc 4: Kiem tra

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## API moi lien quan OTP

### Dang ky va xac thuc email
- `POST /api/v1/accounts/register/student`
- `POST /api/v1/accounts/register/student/verify-otp`
- `POST /api/v1/accounts/register/student/resend-otp`

### Quen mat khau
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

## Tai khoan seed san

| Role | Username | Password |
|------|----------|----------|
| Admin | admin1 | 123456 |
| Admin | admin2 | 123456 |
| Admin | admin3 | 123456 |
| Lecturer | lecturer1 | 123456 |
| Lecturer | lecturer2 | 123456 |
| Lecturer | lecturer3 | 123456 |
| Student | student1 | 123456 |
| Student | student2 | 123456 |
| Student | student3 | 123456 |
