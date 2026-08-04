# Use Case: Token IP Visibility

## Overview

**Use Case ID:** UC-017
**Use Case Name:** Token IP Visibility
**Primary Actor:** Öğrenen
**Goal:** Arayüzde her hatırlanma kaydının hangi IP adresine bağlı olduğunu görerek hırsızlık senaryosunda IP-binding davranışını doğrulamak
**Status:** Verified

## Preconditions

- IP-binding sertleştirmesi etkindir ve en az bir hatırlanma kaydı IP ile ilişkilendirilmiş durumdadır

## Main Success Scenario

1. Öğrenen "Remember Me" seçeneğiyle giriş yapar; kayıt istemci IP'siyle ilişkilendirilir.
2. Öğrenen Token Inspector sayfasını açar.
3. Sistem her kayıt için bağlı olduğu IP adresini diğer bilgilerin (series, token, son kullanım) yanında gösterir.
4. Öğrenen farklı IP'den bir tekrar kullanım denemesi yapar (bkz. UC-016) ve reddedilen isteğin IP'sini kayıttaki IP ile karşılaştırır.
5. Öğrenen, reddin kayıttaki bağlı IP ile isteğin IP'si arasındaki uyuşmazlıktan kaynaklandığını doğrular.

## Alternative Flows

### A1: IP-Binding Devre Dışı

**Trigger:** Kayıtlar herhangi bir IP ile ilişkilendirilmemiştir (step 3)
**Flow:**

1. Sistem IP sütununda kaydın IP'ye bağlı olmadığını açıkça belirtir.
2. Öğrenen IP-binding'i etkinleştirip yeniden giriş yapar.
3. Use case continues at step 2.

## Postconditions

### Success Postconditions

- Öğrenen her kaydın bağlı IP'sini arayüzde görmüş ve red kararını IP uyuşmazlığıyla ilişkilendirmiştir

### Failure Postconditions

- IP bilgisi görüntülenemiyorsa doğrulama yapılamamıştır; sayfa ve yapılandırma incelenmelidir

## Business Rules

### BR-024: IP Bilgisinin Görünürlüğü

IP-binding etkinken her hatırlanma kaydı, bağlı olduğu IP adresiyle birlikte listelenir; bağlı IP yoksa bu durum açıkça gösterilir.
