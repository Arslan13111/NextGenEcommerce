# Supabase Migration Guide - Complete Setup Instructions

## Overview
Your NextGenEcommerce app has been migrated from Firebase to Supabase. This guide will walk you through setting up Supabase for authentication, database, and storage.

---

## Part 1: Create Supabase Project

### Step 1: Sign Up for Supabase
1. Go to [supabase.com](https://supabase.com)
2. Click **"Start your project"** or **"Sign In"**
3. Sign up with GitHub, GitLab, or email

### Step 2: Create a New Project
1. Click **"New Project"**
2. Fill in the details:
   - **Name**: NextGenEcommerce (or your preferred name)
   - **Database Password**: Create a strong password (save this!)
   - **Region**: Choose closest to your users
   - **Pricing Plan**: Free tier is perfect for development
3. Click **"Create new project"**
4. Wait 2-3 minutes for setup to complete

### Step 3: Get Your Project Credentials
1. Once the project is ready, go to **Settings** → **API**
2. You'll need two values:
   - **Project URL** (e.g., `https://xxxxxxxxxxxxx.supabase.co`)
   - **anon public key** (long string starting with `eyJ...`)
3. **Keep these safe** - you'll need them in the next step

---

## Part 2: Configure Your Android App

### Step 1: Add Credentials to Your App
1. Open `SupabaseConfig.kt` located at:
   ```
   app/src/main/java/com/example/nextgenecommerce/data/config/SupabaseConfig.kt
   ```

2. Replace the placeholder values:
   ```kotlin
   private const val SUPABASE_URL = "YOUR_SUPABASE_PROJECT_URL"
   private const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"
   ```

   With your actual values:
   ```kotlin
   private const val SUPABASE_URL = "https://xxxxxxxxxxxxx.supabase.co"
   private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   ```

### Step 2: Sync Gradle
1. Open Android Studio
2. Click **"Sync Now"** when prompted
3. Wait for Gradle sync to complete
4. Build the project to ensure no errors

---

## Part 3: Set Up Database Tables

### Step 1: Create Users Table
1. Go to Supabase Dashboard → **Table Editor**
2. Click **"Create a new table"**
3. Configure the table:
   - **Name**: `users`
   - **Description**: "User profiles"
   - **Enable Row Level Security (RLS)**: ✅ Check this!

4. Add columns (click "+ Add column" for each):

| Column Name        | Type        | Default Value              | Primary | Nullable | Unique |
|-------------------|-------------|----------------------------|---------|----------|--------|
| id                | uuid        | auth.uid()                 | ✅      | ❌       | ✅     |
| email             | text        | -                          | ❌      | ❌       | ✅     |
| name              | text        | -                          | ❌      | ❌       | ❌     |
| phone             | text        | ''                         | ❌      | ✅       | ❌     |
| profile_image_url | text        | null                       | ❌      | ✅       | ❌     |
| created_at        | int8        | extract(epoch from now())  | ❌      | ❌       | ❌     |
| role              | text        | 'customer'                 | ❌      | ❌       | ❌     |

5. Click **"Save"**

### Step 2: Set Up Row Level Security (RLS) Policies

#### For the Users Table:
1. In Table Editor, click on `users` table
2. Click **"Add RLS policy"** or go to **Authentication** → **Policies**
3. Create the following policies:

**Policy 1: Enable read access for users to their own data**
```sql
CREATE POLICY "Users can view their own profile"
ON users FOR SELECT
USING (auth.uid() = id);
```

**Policy 2: Enable insert for authenticated users**
```sql
CREATE POLICY "Users can insert their own profile"
ON users FOR INSERT
WITH CHECK (auth.uid() = id);
```

**Policy 3: Enable update for users to their own data**
```sql
CREATE POLICY "Users can update their own profile"
ON users FOR UPDATE
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);
```

**Quick way using SQL Editor:**
1. Go to **SQL Editor** in Supabase Dashboard
2. Paste and run this:

```sql
-- Enable RLS on users table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy for SELECT
CREATE POLICY "Users can view their own profile"
ON users FOR SELECT
USING (auth.uid() = id);

-- Policy for INSERT
CREATE POLICY "Users can insert their own profile"
ON users FOR INSERT
WITH CHECK (auth.uid() = id);

-- Policy for UPDATE
CREATE POLICY "Users can update their own profile"
ON users FOR UPDATE
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- Policy for DELETE
CREATE POLICY "Users can delete their own profile"
ON users FOR DELETE
USING (auth.uid() = id);
```

---

## Part 4: Set Up Storage Buckets

### Step 1: Create Profile Images Bucket
1. Go to **Storage** in Supabase Dashboard
2. Click **"Create a new bucket"**
3. Configure:
   - **Name**: `profiles`
   - **Public bucket**: ✅ Check this (so profile images are publicly accessible)
4. Click **"Create bucket"**

### Step 2: Set Storage Policies for Profiles Bucket
1. Click on the `profiles` bucket
2. Go to **Policies** tab
3. Click **"New Policy"**

**Policy 1: Public Read Access**
- **Policy name**: Public profiles read access
- **Allowed operation**: SELECT
- **Target roles**: public
- **Policy definition**:
```sql
CREATE POLICY "Public profiles read access"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'profiles');
```

**Policy 2: Authenticated Upload**
- **Policy name**: Authenticated users can upload their profile
- **Allowed operation**: INSERT
- **Target roles**: authenticated
- **Policy definition**:
```sql
CREATE POLICY "Users can upload their profile image"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'profiles' AND
  (storage.foldername(name))[1] = auth.uid()::text
);
```

**Policy 3: Authenticated Update**
- **Policy name**: Users can update their profile image
- **Allowed operation**: UPDATE
- **Target roles**: authenticated
- **Policy definition**:
```sql
CREATE POLICY "Users can update their profile image"
ON storage.objects FOR UPDATE
TO authenticated
USING (
  bucket_id = 'profiles' AND
  (storage.foldername(name))[1] = auth.uid()::text
);
```

**Policy 4: Authenticated Delete**
- **Policy name**: Users can delete their profile image
- **Allowed operation**: DELETE
- **Target roles**: authenticated
- **Policy definition**:
```sql
CREATE POLICY "Users can delete their profile image"
ON storage.objects FOR DELETE
TO authenticated
USING (
  bucket_id = 'profiles' AND
  (storage.foldername(name))[1] = auth.uid()::text
);
```

**Quick SQL approach:**
```sql
-- Allow public read access
CREATE POLICY "Public profiles read access"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'profiles');

-- Allow authenticated users to upload their own profile images
CREATE POLICY "Users can upload their profile image"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'profiles' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

-- Allow users to update their own images
CREATE POLICY "Users can update their profile image"
ON storage.objects FOR UPDATE
TO authenticated
USING (
  bucket_id = 'profiles' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

-- Allow users to delete their own images
CREATE POLICY "Users can delete their profile image"
ON storage.objects FOR DELETE
TO authenticated
USING (
  bucket_id = 'profiles' AND
  (storage.foldername(name))[1] = auth.uid()::text
);
```

---

## Part 5: Set Up Authentication

### Step 1: Configure Email Authentication
1. Go to **Authentication** → **Providers**
2. **Email** should be enabled by default
3. Configure settings:
   - **Enable email confirmations**: Up to you (recommended for production)
   - **Enable email OTP**: Optional
4. Click **"Save"**

### Step 2: Configure Google Authentication (Optional)
1. In **Authentication** → **Providers**, find **Google**
2. Toggle **Enable Google Provider**
3. You'll need:
   - **Client ID** from Google Cloud Console
   - **Client Secret** from Google Cloud Console

**To get Google OAuth credentials:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Go to **APIs & Services** → **Credentials**
4. Click **"Create Credentials"** → **"OAuth 2.0 Client ID"**
5. Configure consent screen if prompted
6. Choose **Android** application type
7. Add your package name: `com.example.nextgenecommerce`
8. Add SHA-1 fingerprint (get from Android Studio)
9. Copy Client ID and Client Secret
10. Paste into Supabase Google Provider settings

### Step 3: Configure Email Templates (Optional)
1. Go to **Authentication** → **Email Templates**
2. Customize:
   - **Confirm signup** email
   - **Magic Link** email
   - **Change Email Address** email
   - **Reset Password** email

---

## Part 6: Testing Your Setup

### Test 1: User Registration
1. Run your Android app
2. Go to registration screen
3. Enter email and password
4. Check Supabase Dashboard → **Authentication** → **Users** to see new user
5. Check **Table Editor** → `users` table to see profile data

### Test 2: User Login
1. Try logging in with the account you just created
2. Should successfully authenticate and load profile

### Test 3: Profile Picture Upload
1. Log in to your app
2. Go to Profile screen
3. Tap on profile picture
4. Select an image from gallery
5. Check Supabase Dashboard → **Storage** → `profiles` bucket
6. You should see the uploaded image in `{userId}/` folder

### Test 4: Profile Update
1. Update user profile information
2. Check **Table Editor** → `users` table
3. Verify the changes are saved

---

## Part 7: Migration Checklist

### Code Changes Completed ✅
- [x] Updated `build.gradle.kts` with Supabase dependencies
- [x] Created `SupabaseConfig.kt` for client initialization
- [x] Updated `AppModule.kt` with Supabase providers
- [x] Migrated `AuthRepository.kt` to use Supabase Auth
- [x] Migrated `StorageRepository.kt` to use Supabase Storage
- [x] Updated `User.kt` model with Kotlin serialization
- [x] Updated `AuthViewModel.kt` for async logout

### Supabase Setup Required ⚠️
You need to complete these steps in Supabase Dashboard:
- [ ] Create Supabase project
- [ ] Copy Project URL and anon key to `SupabaseConfig.kt`
- [ ] Create `users` table with proper columns
- [ ] Set up Row Level Security policies for `users` table
- [ ] Create `profiles` storage bucket
- [ ] Set up storage policies for `profiles` bucket
- [ ] Configure email authentication
- [ ] (Optional) Configure Google authentication
- [ ] Test registration, login, and profile upload

---

## Part 8: Important Notes

### Security
- **Never commit** your Supabase URL and anon key to public repositories
- Consider using BuildConfig or local.properties for credentials
- Always use Row Level Security (RLS) for database tables
- Review and test all security policies before going to production

### Storage URLs
- Profile images are stored at: `{supabase-url}/storage/v1/object/public/profiles/{userId}/{filename}`
- Public buckets are accessible without authentication
- Private buckets require signed URLs

### Database Schema
- The `users` table is separate from Supabase Auth
- Auth handles authentication; `users` table stores profile data
- `id` in `users` table matches `auth.uid()` from Supabase Auth

### Cost Considerations
Supabase Free Tier includes:
- 500 MB database space
- 1 GB file storage
- 2 GB bandwidth per month
- 50,000 monthly active users
- Unlimited API requests

---

## Part 9: Troubleshooting

### Issue: "Invalid API key"
**Solution**:
- Double-check your SUPABASE_URL and SUPABASE_ANON_KEY in `SupabaseConfig.kt`
- Make sure there are no extra spaces or quotes
- Get fresh credentials from Supabase Dashboard → Settings → API

### Issue: "Row Level Security policy violation"
**Solution**:
- Check that RLS policies are correctly set up
- Verify the user is authenticated before accessing data
- Use SQL Editor to test policies manually

### Issue: "Storage upload failed"
**Solution**:
- Verify the bucket exists and is public (if needed)
- Check storage policies allow the operation
- Ensure file size is within limits (50 MB per file in free tier)

### Issue: "Failed to insert into users table"
**Solution**:
- Make sure the user is authenticated
- Verify the `id` field matches `auth.uid()`
- Check that required fields are not null

### Issue: "Google Sign-In not working"
**Solution**:
- Verify Google OAuth is enabled in Supabase
- Check Client ID and Secret are correct
- Ensure SHA-1 fingerprint is added to Google Cloud Console
- Test with proper redirect URLs configured

---

## Part 10: Additional Resources

### Documentation
- [Supabase Docs](https://supabase.com/docs)
- [Supabase Auth Guide](https://supabase.com/docs/guides/auth)
- [Supabase Storage Guide](https://supabase.com/docs/guides/storage)
- [Row Level Security](https://supabase.com/docs/guides/auth/row-level-security)

### Supabase Kotlin Client
- [GitHub Repository](https://github.com/supabase-community/supabase-kt)
- [Documentation](https://supabase.com/docs/reference/kotlin/introduction)

### Community
- [Supabase Discord](https://discord.supabase.com/)
- [Supabase GitHub Discussions](https://github.com/supabase/supabase/discussions)

---

## Summary

Your app has been successfully migrated from Firebase to Supabase! The main changes:

1. **Authentication**: Using Supabase Auth (GoTrue)
2. **Database**: PostgreSQL instead of Firestore
3. **Storage**: Supabase Storage instead of Firebase Storage
4. **Real-time** (optional): Available via Supabase Realtime

Follow the steps in this guide to complete the Supabase setup, and you'll be ready to run your app with Supabase as the backend!

**Next Steps:**
1. Complete Part 2 (add credentials to `SupabaseConfig.kt`)
2. Complete Part 3 (create database tables)
3. Complete Part 4 (set up storage buckets)
4. Complete Part 5 (configure authentication)
5. Test everything (Part 6)

Good luck! 🚀
