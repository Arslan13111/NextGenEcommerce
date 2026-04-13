# NextGenEcommerce

NextGenEcommerce is an Android e-commerce project with a separate Node.js backend for AI virtual try-on. This folder contains the mobile app, backend API, SQL/setup files for Supabase, and project notes documenting major fixes and migrations.

## What is done in this folder

### Android app
- Android app built with Kotlin
- Jetpack Compose UI with navigation-based multi-screen flow
- Hilt dependency injection
- Room local database
- Supabase integration for authentication, database, storage, and realtime
- Product browsing, product detail, cart, wishlist, checkout, orders, notifications, profile, settings, and admin screens
- AI virtual try-on flow with camera/image selection support
- ARCore, CameraX, ML Kit, and TensorFlow Lite dependencies included for advanced try-on/vision features

### Backend
- Express.js backend inside `backend/`
- `/api/try-on` endpoint for AI try-on using Hugging Face IDM-VTON
- Additional sample APIs for products, orders, auth, and reviews
- Health endpoint for backend status checks

### Migration and fixes already documented
- Firebase to Supabase migration notes
- Registration/login fixes
- Email confirmation flow update
- Error-handling and user-deletion notes
- Supabase SQL setup and migration files

## Main project structure

```text
app/                Android application source
backend/            Node.js Express backend
gradle/             Gradle wrapper and version config
API/                API screenshots/examples
*.md                Project notes, fixes, and migration guides
*.sql               Supabase setup and policy scripts
```

## Important files

- `app/build.gradle.kts` - Android app dependencies and SDK configuration
- `app/src/main/java/com/example/nextgenecommerce/` - Android source code
- `backend/server.js` - backend server and API routes
- `SUPABASE_MIGRATION_GUIDE.md` - Supabase setup guide
- `MIGRATION_SUMMARY.md` - migration summary
- `REGISTRATION_FLOW_UPDATE.md` - registration flow changes

## Tech stack

- Android, Kotlin, Jetpack Compose
- Hilt
- Room
- Supabase
- Retrofit, OkHttp, Ktor
- CameraX, ARCore
- ML Kit, TensorFlow Lite
- Node.js, Express
- Hugging Face Spaces IDM-VTON

## Local setup

### Android app
1. Open the project in Android Studio.
2. Sync Gradle.
3. Review `local.properties`.
4. Add the required Supabase configuration in the Android source if it is still placeholder-based.
5. Run the app on an emulator or Android device.

### Backend
1. Open a terminal in `backend/`
2. Install dependencies:

```bash
npm install
```

3. Start the server:

```bash
npm start
```

## Before uploading to GitHub

Check these items first:

- Do not upload `backend/node_modules/`
- Do not upload `build/` or `app/build/`
- Do not upload `.env` files
- Do not upload `local.properties`
- Do not upload `google-services.json` unless you intentionally want it public
- Review SQL files and docs for any real credentials before pushing

The `.gitignore` in this folder has been updated to help with that.

## How to upload this project to GitHub

This folder is currently not initialized as a Git repository, so use these commands from the project root:

```bash
git init
git branch -M main
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/NextGenEcommerce.git
git push -u origin main
```

## Create the GitHub repository first

1. Go to GitHub
2. Click `New repository`
3. Set the repository name to `NextGenEcommerce`
4. Create it without adding another README if you want to push this folder directly
5. Copy the repository URL and use it in the `git remote add origin ...` command above

## Recommended next cleanup before pushing

- Remove generated files that are not part of source control
- Make sure secrets are not hardcoded
- If `backend/node_modules/` is already present locally, keep it local only
- Consider removing unused temporary folders before the first push

## Status summary

This folder already contains a substantial Android e-commerce app, a backend for virtual try-on, Supabase migration work, and multiple implementation/fix documents. It is suitable to upload to GitHub after confirming no secrets or generated files should be included.
