# Use Case: IP-Bound Remember-Me

## Overview

**Use Case ID:** UC-016
**Use Case Name:** IP-Bound Remember-Me
**Primary Actor:** Öğrenen
**Goal:** Remember-me token'ının üretildiği istemci IP'sine bağlandığını ve farklı bir IP'den tekrar kullanımının reddedildiğini gözlemlemek
**Status:** Draft

## Preconditions

- IP-binding sertleştirmesi yapılandırmada etkinleştirilmiştir
- Öğrenen, farklı IP adreslerinden istek gönderebileceği iki istemci ortamına sahiptir

## Main Success Scenario

1. Öğrenen birinci istemciden "Remember Me" seçeneğiyle giriş yapar.
2. Sistem hatırlanma kaydını, isteğin geldiği istemci IP adresiyle ilişkilendirir.
3. Öğrenen remember-me cookie'sini ikinci (farklı IP'li) istemciye kopyalayarak hırsızlığı simüle eder.
4. Öğrenen ikinci istemciden korumalı bir sayfaya istek yapar.
5. Sistem isteğin IP adresinin kayıttaki IP ile uyuşmadığını tespit eder.
6. Sistem otomatik girişi reddeder ve login ister.
7. Öğrenen, IP bağlamanın farklı IP'den tekrar kullanımı engellediğini doğrular.

## Alternative Flows

### A1: Aynı IP'den İstek

**Trigger:** İstek, kaydın bağlı olduğu IP adresinden gelmektedir (step 5)
**Flow:**

1. Sistem otomatik girişi kabul eder; öğrenen normal remember-me davranışını görür.
2. Use case ends.

### A2: IP-Binding Devre Dışı

**Trigger:** Yapılandırmada IP-binding kapalı olduğu için IP denetimi yapılmaz (step 5)
**Flow:**

1. Sistem farklı IP'den gelen isteği de kabul eder.
2. Öğrenen yapılandırmada IP-binding'i etkinleştirir ve uygulamayı yeniden başlatır.
3. Use case continues at step 1.

## Postconditions

### Success Postconditions

- Farklı IP'den yapılan otomatik giriş denemesi reddedilmiştir
- Öğrenen ek sertleştirme katmanının etkisini gözlemlemiştir

### Failure Postconditions

- Farklı IP'den gelen istek kabul edilmişse IP bağlama çalışmamaktadır; yapılandırma incelenmelidir

## Business Rules

### BR-023: IP Eşleşme Zorunluluğu

IP-binding etkinken remember-me ile otomatik giriş yalnızca kaydın üretildiği IP adresinden kabul edilir; diğer tüm IP'lerden gelen denemeler reddedilir.
