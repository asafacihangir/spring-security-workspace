# Use Case: Re-Authentication

## Overview

**Use Case ID:** UC-009
**Use Case Name:** Re-Authentication
**Primary Actor:** Not Kullanıcısı
**Goal:** Şifresini yeniden girerek hatırlanan oturumunu tam kimlik doğrulamaya yükseltmek ve Account Settings sayfasına erişmek
**Status:** Verified

## Preconditions

- Kullanıcının seviyesi "Remembered"tır
- Kullanıcı tam kimlik doğrulama gerektiren bir sayfaya (Account Settings) erişmeye çalışmış ve yönlendirilmiştir

## Main Success Scenario

1. Sistem yeniden kimlik doğrulama sayfasını gösterir.
2. Kullanıcı şifresini girer ve gönderir.
3. Sistem şifreyi mevcut kullanıcının hesabına göre doğrular.
4. Sistem kullanıcının seviyesini "Fully Authenticated"a yükseltir.
5. Sistem kullanıcıyı başlangıçta erişmek istediği Account Settings sayfasına yönlendirir; kullanıcı sayfaya erişir.

## Alternative Flows

### A1: Yanlış Şifre

**Trigger:** Girilen şifre doğrulanamaz (step 3)
**Flow:**

1. Sistem hata mesajı gösterir; seviye "Remembered" olarak kalır.
2. Use case continues at step 2.

### A2: Kullanıcı Vazgeçer

**Trigger:** Kullanıcı yeniden doğrulama sayfasından ayrılır (step 2)
**Flow:**

1. Kullanıcı başka bir sayfaya gider; seviye "Remembered" olarak kalır.
2. Use case ends.

## Postconditions

### Success Postconditions

- Kullanıcının seviyesi "Fully Authenticated"tır
- Kullanıcı Account Settings sayfasına erişmiştir

### Failure Postconditions

- Seviye "Remembered" olarak kalmıştır; Account Settings erişimi hâlâ kapalıdır

## Business Rules

### BR-012: Aynı Hesapla Yükseltme

Seviye yükseltme yalnızca hatırlanan kullanıcının kendi şifresiyle yapılır; farklı bir hesabın bilgileriyle yükseltme yapılamaz.
