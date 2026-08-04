# Use Case: Remember-Me Opt-In

## Overview

**Use Case ID:** UC-002
**Use Case Name:** Remember-Me Opt-In
**Primary Actor:** Not Kullanıcısı
**Goal:** Login sırasında "Remember Me" seçeneğini işaretleyerek, oturum sona erse bile hatırlanmayı sağlamak
**Status:** Verified

## Preconditions

- Kayıtlı bir demo kullanıcı hesabı mevcuttur
- Kullanıcı login sayfasındadır

## Main Success Scenario

1. Sistem, "Remember Me" seçeneğini içeren login formunu gösterir.
2. Kullanıcı kullanıcı adını ve şifresini girer.
3. Kullanıcı "Remember Me" seçeneğini işaretler.
4. Kullanıcı formu gönderir.
5. Sistem kimlik bilgilerini doğrular.
6. Sistem oturum başlatır ve kullanıcı için bir remember-me cookie'si üretir.
7. Sistem kullanıcıyı notlar sayfasına yönlendirir; kullanıcı artık oturum kaybında da hatırlanacaktır.

## Alternative Flows

### A1: Seçenek İşaretlenmemiş

**Trigger:** Kullanıcı "Remember Me" seçeneğini işaretlememiştir (step 3)
**Flow:**

1. Kullanıcı formu seçeneği işaretlemeden gönderir.
2. Sistem kimlik bilgilerini doğrular ve yalnızca oturum başlatır; remember-me cookie'si üretilmez.
3. Sistem kullanıcıyı notlar sayfasına yönlendirir.
4. Use case ends.

### A2: Kimlik Doğrulama Başarısız

**Trigger:** Girilen kimlik bilgileri doğrulanamaz (step 5)
**Flow:**

1. Sistem hata mesajı gösterir; oturum da remember-me cookie'si de üretilmez.
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- Kullanıcının aktif bir oturumu ve tarayıcısında bir remember-me cookie'si vardır
- Oturum sona erdiğinde kullanıcı otomatik olarak yeniden doğrulanabilir durumdadır

### Failure Postconditions

- Remember-me cookie'si üretilmemiştir
- Kullanıcı login sayfasında kalır veya yalnızca oturuma bağlı şekilde giriş yapmıştır

## Business Rules

### BR-003: Açık Rıza

Remember-me cookie'si yalnızca kullanıcı login formundaki seçeneği açıkça işaretlediğinde üretilir; varsayılan davranış hatırlamamaktır.

### BR-004: Cookie Koruması

Oturum ve remember-me cookie'leri istemci tarafı betikler tarafından okunamayacak şekilde üretilir.
