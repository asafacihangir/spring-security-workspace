# Use Case: Form Login

## Overview

**Use Case ID:** UC-001
**Use Case Name:** Form Login
**Primary Actor:** Not Kullanıcısı
**Goal:** Kullanıcı adı ve şifresiyle giriş yaparak kişisel notlarına erişmek
**Status:** Draft

## Preconditions

- Kayıtlı bir demo kullanıcı hesabı mevcuttur
- Kullanıcı henüz kimlik doğrulaması yapmamıştır (Anonymous seviyesindedir)

## Main Success Scenario

1. Kullanıcı login sayfasını açar.
2. Sistem kullanıcı adı ve şifre alanları ile isteğe bağlı "Remember Me" seçeneğini içeren formu gösterir.
3. Kullanıcı kullanıcı adını ve şifresini girer.
4. Kullanıcı formu gönderir.
5. Sistem kimlik bilgilerini doğrular.
6. Sistem kullanıcı için oturum başlatır ve notlar sayfasına yönlendirir.
7. Sistem kullanıcının kimlik doğrulama seviyesini "Fully Authenticated" olarak gösterir; kullanıcı kişisel notlarına erişir.

## Alternative Flows

### A1: Hatalı Kimlik Bilgileri

**Trigger:** Girilen kullanıcı adı veya şifre doğrulanamaz (step 5)
**Flow:**

1. Sistem genel bir hata mesajı gösterir; hangi alanın hatalı olduğunu belirtmez.
2. Sistem şifre alanını temizler.
3. Use case continues at step 3.

### A2: Eksik Alanlar

**Trigger:** Kullanıcı adı veya şifre alanı boş bırakılmıştır (step 4)
**Flow:**

1. Sistem eksik alanları belirterek formu yeniden gösterir.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- Kullanıcının aktif bir oturumu vardır ve seviyesi "Fully Authenticated"tır
- Kullanıcı notlar sayfasındadır ve kişisel notlarına erişebilir

### Failure Postconditions

- Oturum başlatılmamıştır; kullanıcı Anonymous seviyesinde kalır
- Kullanıcı login sayfasındadır ve hata mesajını görür

## Business Rules

### BR-001: Tek Demo Kullanıcı Rolü

Giriş yalnızca kayıtlı demo kullanıcı hesabıyla yapılabilir; tüm kullanıcılar tek `USER` rolüne sahiptir, rol hiyerarşisi yoktur.

### BR-002: Genel Hata Mesajı

Başarısız girişte sistem, kullanıcı adının mı şifrenin mi hatalı olduğunu açıklamaz; tek bir genel hata mesajı gösterir.
