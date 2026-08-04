# Use Case: Expired Token Cleanup

## Overview

**Use Case ID:** UC-014
**Use Case Name:** Expired Token Cleanup
**Primary Actor:** Öğrenen
**Goal:** Zamanlanmış arka plan işinin süresi dolmuş kalıcı hatırlanma kayıtlarını sildiğini ve kayıt tablosunun sonsuza kadar büyümediğini gözlemlemek
**Status:** Verified

## Preconditions

- Persistent-based remember-me stratejisi aktiftir
- Zamanlanmış temizlik işi yapılandırılmış ve etkin durumdadır

## Main Success Scenario

1. Öğrenen, kısa geçerlilik süresiyle "Remember Me" girişleri yaparak kalıcı kayıtlar oluşturur.
2. Öğrenen geçerlilik süresi dolana kadar bekler; kayıtlar süresi dolmuş duruma gelir.
3. Zamanlanmış temizlik işi planlanan zamanında çalışır.
4. Sistem süresi dolmuş tüm kalıcı kayıtları siler; süresi dolmamış kayıtlara dokunmaz.
5. Öğrenen Token Inspector sayfasında süresi dolmuş kayıtların silindiğini doğrular.

## Alternative Flows

### A1: Veritabanı Erişilemez

**Trigger:** Temizlik işi çalıştığında veritabanına ulaşılamaz (step 4)
**Flow:**

1. Sistem hatayı günlüğe kaydeder; uygulama çalışmaya ve istekleri karşılamaya devam eder.
2. Temizlik, bir sonraki planlı çalıştırmada yeniden denenir.
3. Use case ends.

### A2: Silinecek Kayıt Yok

**Trigger:** Çalışma anında süresi dolmuş kayıt bulunmamaktadır (step 4)
**Flow:**

1. İş hiçbir kaydı silmeden tamamlanır.
2. Use case ends.

## Postconditions

### Success Postconditions

- Süresi dolmuş kalıcı kayıtlar silinmiştir; geçerli kayıtlar korunmuştur
- Öğrenen temizlik davranışını Token Inspector üzerinden doğrulamıştır

### Failure Postconditions

- Süresi dolmuş kayıtlar yerinde kalmıştır; hata günlüğe kaydedilmiştir ve uygulama kesintisiz çalışmaya devam etmektedir

## Business Rules

### BR-020: Yalnızca Süresi Dolmuş Kayıtlar

Temizlik işi yalnızca geçerlilik süresi dolmuş kayıtları siler; geçerli kayıtlar hiçbir koşulda silinmez.

### BR-021: Kesintisiz Hizmet

Temizlik işindeki bir hata uygulamanın istek karşılamasını durdurmaz; uygulama yeniden başlatma gerektirmeden çalışmaya devam eder.
