# Remember-Me Lab

## Mimari

- **Backend:** `applications/backend` — Java 21, Spring Boot 3.x
  (web, security, data-jpa, mysql-connector-j).
- **Frontend:** `applications/frontend` — React 18 + Vite, backend'e
  `/api` altında proxy'lenen sade bir `fetch` istemcisi (JWT/token katmanı yok,
  cookie tabanlı session).
- **Altyapı:** `infra.yml` ile Docker Compose üzerinden MySQL.
- **Görevler:** `Taskfile.yml` ile `task` üzerinden çalıştırılır.

## Gereksinimler

- Java 21 (`sdk install java 21.0.2-tem` veya eşdeğeri)
- Maven Wrapper (repo içinde, ayrıca kurulum gerekmez)
- Node.js 18+ ve npm
- Docker + Docker Compose
- [go-task](https://taskfile.dev) (`brew install go-task`)

## Çalıştırma

```bash
# 1) MySQL'i ayağa kaldır
task infra:up

# 2) Backend'i başlat (http://localhost:8080)
task backend:run

# 3) Frontend'i başlat (http://localhost:5173)
task frontend:run
```

Frontend açıldığında (`http://localhost:5173`) bir login formu (kullanıcı
adı, şifre, "Beni hatırla" kutusu) ve altında bir "Token Inspector" butonu
görürsün. Giriş için demo hesabı kullan:

- **Kullanıcı adı:** `demo`
- **Şifre:** `password123`

Bu hesap `DemoUserSeeder` tarafından backend her başladığında otomatik
olarak seed edilir (henüz yoksa oluşturulur - bkz.
`DemoUserSeeder.java`); ayrıca elle bir kayıt oluşturman gerekmez.
