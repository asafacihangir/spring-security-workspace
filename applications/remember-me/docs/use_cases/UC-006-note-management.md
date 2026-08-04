# Use Case: Note Management

## Overview

**Use Case ID:** UC-006
**Use Case Name:** Note Management
**Primary Actor:** Not Kullanıcısı
**Goal:** Kişisel notlarını oluşturmak, listelemek, güncellemek ve silmek
**Status:** Verified

## Preconditions

- Kullanıcı giriş yapmış durumdadır

## Main Success Scenario

1. Kullanıcı notlar sayfasını açar.
2. Sistem kullanıcının kendi notlarını listeler.
3. Kullanıcı "Yeni Not" seçeneğini seçer.
4. Sistem not formunu gösterir.
5. Kullanıcı başlık ve içerik girip kaydeder.
6. Sistem notu saklar ve güncellenmiş listeyi gösterir; kullanıcı notunu listede görür.

## Alternative Flows

### A1: Not Güncelleme

**Trigger:** Kullanıcı listedeki bir notu düzenlemek ister (step 2)
**Flow:**

1. Kullanıcı notu seçer ve düzenler.
2. Sistem değişiklikleri saklar.
3. Use case continues at step 2.

### A2: Not Silme

**Trigger:** Kullanıcı listedeki bir notu silmek ister (step 2)
**Flow:**

1. Kullanıcı notu seçer ve silmeyi onaylar.
2. Sistem notu kalıcı olarak siler.
3. Use case continues at step 2.

### A3: Geçersiz Not İçeriği

**Trigger:** Başlık boş bırakılmıştır (step 5)
**Flow:**

1. Sistem eksik alanı belirten bir hata mesajı gösterir; not saklanmaz.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- Yapılan oluşturma/güncelleme/silme işlemi kalıcı olarak saklanmıştır
- Liste, kullanıcının notlarının güncel durumunu yansıtır

### Failure Postconditions

- Geçersiz işlem saklanmamıştır; mevcut notlar değişmeden kalır

## Business Rules

### BR-008: Not Sahipliği

Bir kullanıcı yalnızca kendi notlarını görebilir ve yönetebilir; başka kullanıcıların notlarına erişemez.
