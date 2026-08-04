# Use Case: Auth Level Indicator

## Overview

**Use Case ID:** UC-007
**Use Case Name:** Auth Level Indicator
**Primary Actor:** Öğrenen
**Goal:** Arayüzdeki göstergeden anlık kimlik doğrulama seviyesini (Anonymous / Remembered / Fully Authenticated) izleyerek üç seviye arasındaki farkı gözlemlemek
**Status:** Draft

## Preconditions

- Uygulama çalışır durumdadır
- Kayıtlı bir demo kullanıcı hesabı mevcuttur

## Main Success Scenario

1. Öğrenen uygulamayı giriş yapmadan açar.
2. Sistem kimlik doğrulama göstergesinde "Anonymous" seviyesini gösterir.
3. Öğrenen "Remember Me" seçeneğiyle giriş yapar.
4. Sistem göstergede "Fully Authenticated" seviyesini gösterir.
5. Öğrenen oturumunu sonlandırıp korumalı bir sayfaya yeniden istek yapar; otomatik giriş gerçekleşir.
6. Sistem göstergede "Remembered" seviyesini gösterir.
7. Öğrenen üç seviyenin göstergede ayrıştığını gözlemler.

## Alternative Flows

### A1: Otomatik Giriş Gerçekleşmedi

**Trigger:** Remember-me cookie'si bulunmadığı için otomatik giriş yapılamaz (step 5)
**Flow:**

1. Sistem öğreneni login sayfasına yönlendirir ve gösterge "Anonymous" olur.
2. Öğrenen "Remember Me" seçeneğiyle yeniden giriş yapar.
3. Use case continues at step 5.

## Postconditions

### Success Postconditions

- Öğrenen üç kimlik doğrulama seviyesinin her birini göstergede görmüştür
- Gösterge, anlık kimlik doğrulama durumunu doğru yansıtmaktadır

### Failure Postconditions

- Gösterge gerçek seviyeyle uyuşmuyorsa gözlem geçersizdir; uygulama davranışı incelenmelidir

## Business Rules

### BR-009: Tek ve Doğru Seviye

Gösterge her an üç seviyeden tam olarak birini gösterir ve bu seviye sistemin kullanıcıya o an tanıdığı gerçek kimlik doğrulama durumuyla aynıdır.
