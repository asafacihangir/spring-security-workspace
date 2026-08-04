# Use Case: Sensitive Page Protection

## Overview

**Use Case ID:** UC-008
**Use Case Name:** Sensitive Page Protection
**Primary Actor:** Not Kullanıcısı
**Goal:** Account Settings sayfasına yalnızca tam kimlik doğrulamayla erişmek; hatırlanan bir oturumun hesap bilgilerini değiştirememesini güvence altına almak
**Status:** Draft

## Preconditions

- Kullanıcı hesabı mevcuttur
- Account Settings sayfası tam kimlik doğrulama gerektirecek şekilde korunmaktadır

## Main Success Scenario

1. Tam kimlik doğrulamayla giriş yapmış kullanıcı Account Settings sayfasını açmak ister.
2. Sistem kullanıcının kimlik doğrulama seviyesini denetler.
3. Sistem seviyenin "Fully Authenticated" olduğunu doğrular ve sayfayı gösterir.
4. Kullanıcı hesap bilgilerini görüntüler ve değiştirebilir; korumalı sayfaya erişim sağlanmıştır.

## Alternative Flows

### A1: Remembered Seviyede Erişim Denemesi

**Trigger:** Kullanıcının seviyesi "Remembered"tır (step 2)
**Flow:**

1. Sistem sayfayı göstermez.
2. Sistem kullanıcıyı yeniden kimlik doğrulama sayfasına yönlendirir (bkz. UC-009).
3. Use case ends.

### A2: Anonim Erişim Denemesi

**Trigger:** Kullanıcı giriş yapmamıştır (step 2)
**Flow:**

1. Sistem kullanıcıyı login sayfasına yönlendirir.
2. Use case ends.

## Postconditions

### Success Postconditions

- Account Settings sayfası yalnızca tam kimlik doğrulamalı kullanıcıya gösterilmiştir

### Failure Postconditions

- Sayfa gösterilmemiştir; hesap bilgilerinde hiçbir değişiklik yapılmamıştır
- Kullanıcı, seviyesine uygun sayfaya (login veya yeniden doğrulama) yönlendirilmiştir

## Business Rules

### BR-010: Tam Doğrulama Zorunluluğu

Account Settings sayfası yalnızca tam kimlik doğrulamayla erişilebilir; hatırlanan (Remembered) seviye yeterli değildir.

### BR-011: Hesap Değişikliği Kısıtı

Hatırlanan bir oturum, hesap bilgilerini görüntüleyemez ve değiştiremez; değişiklik ancak şifreyle doğrulanmış bir oturumdan yapılabilir.
