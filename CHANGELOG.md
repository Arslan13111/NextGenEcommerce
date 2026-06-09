# Changelog

## 2026-06-09

### Android application

- Added Firebase Cloud Messaging registration, token storage, notification handling, and admin/retailer notification screens.
- Expanded admin workflows for retailer and delivery-partner approval, product oversight, returns, revenue, commissions, and the admin vault.
- Redesigned retailer and delivery dashboards with order, return, approval, earnings, profile, and product-management workflows.
- Added customer wallet/vault support and integrated it into checkout and payment handling.
- Improved Safepay checkout with server-created sessions, deep-link handling, payment verification, and status tracking.
- Added legal, privacy, terms, help, account-deletion, and vault screens and navigation routes.
- Updated order, product, retailer, user, notification, repository, Room, and ViewModel layers for the new workflows.
- Refined live camera and image-based virtual try-on integrations while removing unused ARCore, Sceneform, ML Kit, and TensorFlow dependencies.

### Backend and database

- Added authenticated order APIs, authorization checks, server-side price validation, account deletion, and protected payment operations.
- Added Safepay session and webhook processing with secrets kept on the server.
- Added public privacy-policy, terms, and account-deletion pages.
- Added Supabase migrations for notifications, FCM tokens, wallet transactions, approval/rejection states, returns, delivery access, Safepay fields, and release security hardening.
- Strengthened row-level security policies and protected role, approval, order, wallet, storage, and payment updates.

### Build and security

- Moved Supabase, Google, Snap Camera Kit, TryOn, RapidAPI, backend, and Safepay configuration to `local.properties` or CI environment variables.
- Added safe example configuration files for Android and backend setup.
- Disabled cleartext traffic and verbose HTTP logging for release builds.
- Updated the project to Android SDK 35, Android Gradle Plugin 8.13.0, and newer backend dependencies.
