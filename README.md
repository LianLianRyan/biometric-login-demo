# 🔐 Biometric Login Demo (WebAuthn + JWT + Angular + Spring Boot)

This full-stack demo showcases how to implement **biometric login using WebAuthn**, combined with **JWT-based session management** and **Redis-backed refresh token rotation**.

> ✅ Aimed at showcasing secure login best practices using modern frontend and backend tech.

---

## 🚀 Tech Stack

| Layer         | Technology                          |
|--------------|--------------------------------------|
| Frontend     | Angular 17, Signals, Standalone APIs |
| Backend      | Spring Boot 3.x                      |
| Auth API     | WebAuthn (`navigator.credentials.*`) |
| Session      | JWT (access + refresh)               |
| Token Store  | Redis                                |
| Auth Flow    | Cookie-based, HttpOnly, SameSite     |

---

## 📦 Features

### ✅ WebAuthn Biometric Login
- Uses `navigator.credentials.create()` and `navigator.credentials.get()` for passwordless biometric auth.
- Tested with Face ID, Touch ID, Windows Hello.

### ✅ Dual JWT Token System
- **Access token** (15 min): short-lived, for regular requests
- **Refresh token** (7 days): securely stored in Redis with JTI, used to issue new access tokens

### ✅ Secure Cookie Handling
- Tokens delivered via **HttpOnly**, **SameSite=Strict** cookies
- Refresh token scoped to `/auth/refresh` path only

### ✅ Stateless API Authentication
- JWT parsed and verified on each request
- Redis used to control refresh token rotation and invalidation
