# Remember-Me Lab

Spring Security'nin token-based ve persistent-token remember-me stratejilerini
uçtan uca deneyebileceğimiz bir demo uygulama. Kullanıcı adı/şifre ile giriş
yapar, notlarını yönetir, "Remember Me" ile oturumu hayatta tutar ve
Token Inspector üzerinden arka planda ne olduğunu izler.

Bu commit yalnızca **Faz 0 — Altyapı ve İskelet**'i içerir: kimlik doğrulama
mantığı henüz yok, sadece MySQL + backend + frontend'in birlikte ayağa
kalktığı boş bir iskelet var. Sonraki fazlar için bkz.
[docs/plan.md](docs/plan.md).

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

Frontend açıldığında "Check backend health" butonuna basarak backend'e
`/api/health` isteği atabilirsin; Vite dev server bu isteği proxy ile
`http://localhost:8080/api/health` adresine yönlendirir.

## Test

```bash
task backend:test
```

Backend testleri JPA context'i ayağa kaldırdığı için `task infra:up` ile
MySQL'in çalışıyor olması gerekir.

## Görevler

| Görev              | Açıklama                                   |
| ------------------ | ------------------------------------------- |
| `task infra:up`    | MySQL container'ını başlatır                |
| `task infra:down`  | MySQL container'ını durdurur                |
| `task backend:run` | Spring Boot backend'i çalıştırır            |
| `task backend:test`| Backend testlerini çalıştırır               |
| `task frontend:run`| Vite dev server'ı çalıştırır                |

## Dokümantasyon

- [vision.md](docs/vision.md) — ürün vizyonu
- [requirements.md](docs/requirements.md) — fonksiyonel/teknik gereksinimler
- [plan.md](docs/plan.md) — faz faz uygulama planı
- [use_cases/](docs/use_cases/) — use case'ler
