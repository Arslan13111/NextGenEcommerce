# Solution: Email Already In Use After Deletion

## Your Problem

You deleted user `arslanmunawar1311@gmail.com` from Supabase, but when trying to register with the same email, you get:
```
"This email is already in use"
```

## Why This Happens

Supabase has **TWO SEPARATE TABLES** for user data:

### 1. `auth.users` (Authentication Table)
- **Location**: Authentication → Users in Supabase Dashboard
- **Purpose**: Stores authentication data (email, password hash, email confirmation)
- **Managed by**: Supabase Auth
- **What you did**: ❌ NOT deleted from here

### 2. `users` (Profile Table)
- **Location**: Table Editor → users
- **Purpose**: Stores profile data (name, phone, profile image, etc.)
- **Managed by**: Your app
- **What you did**: ✅ Deleted from here

**Result**: Email still exists in `auth.users`, so Supabase thinks it's taken!

---

## The Solution

You need to delete the user from **BOTH** tables.

### Step 1: Open Supabase SQL Editor
1. Go to your Supabase Dashboard
2. Click **SQL Editor** in the left sidebar
3. Click **New Query**

### Step 2: Run This SQL

Copy and paste this exactly:

```sql
-- Delete from Authentication table (this is the one you missed!)
DELETE FROM auth.users WHERE email = 'arslanmunawar1311@gmail.com';

-- Delete from Profile table (you probably already did this, but just to be sure)
DELETE FROM users WHERE email = 'arslanmunawar1311@gmail.com';

-- Verify both deletions worked
SELECT COUNT(*) as auth_users_count FROM auth.users WHERE email = 'arslanmunawar1311@gmail.com';
SELECT COUNT(*) as users_count FROM users WHERE email = 'arslanmunawar1311@gmail.com';
```

Both counts should be **0**.

### Step 3: Click "Run" Button

The SQL will execute and delete the user from both tables.

### Step 4: Try Registering Again

Now you can register with `arslanmunawar1311@gmail.com` successfully!

---

## How It Works Now (After Our Fixes)

When someone tries to register with an email that exists in `auth.users`:

1. **Old behavior**:
   ```
   Error: Unexpected JSON token at offset 177: Encountered an unknown key...
   ```

2. **New behavior** (after our fixes):
   ```
   ┌────────────────────────────────────────────┐
   │ ⚠️ This email is already registered.      │
   │    Please login instead.                   │
   └────────────────────────────────────────────┘
   ```

This error is **correct** - it means the email exists in Supabase Auth and you need to delete it!

---

## What We Fixed in AuthRepository.kt

Updated the registration function to catch authentication errors:

```kotlin
// In AuthRepository.kt register function
val result = try {
    supabaseAuth.signUpWith(Email) {
        this.email = email
        this.password = password
    }
} catch (authError: Exception) {
    // Check if it's a "user already exists" error
    when {
        authError.message?.contains("already registered", ignoreCase = true) == true ||
        authError.message?.contains("already exists", ignoreCase = true) == true ||
        authError.message?.contains("duplicate", ignoreCase = true) == true ->
            throw Exception("This email is already registered. Please login instead.")
        else ->
            throw authError
    }
}
```

This provides a **clear, user-friendly error message** instead of cryptic JSON errors.

---

## Preventing This in the Future

### Option 1: Manual Deletion (What you should do now)
Always delete from both places:
1. **Authentication → Users** (delete the user)
2. **Table Editor → users** (delete the row)

### Option 2: Automatic Deletion (Optional - Add a Trigger)
You can create a trigger that automatically deletes from the `users` table when someone is deleted from `auth.users`:

```sql
-- Create a function to handle cascading delete
CREATE OR REPLACE FUNCTION handle_user_delete()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM users WHERE id = OLD.id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create trigger
CREATE TRIGGER on_auth_user_deleted
    BEFORE DELETE ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION handle_user_delete();
```

After adding this trigger, deleting from Authentication will automatically delete from the users table!

---

## Quick Reference

| Task | How To Do It |
|------|-------------|
| **Delete completely** | Run both DELETE statements in SQL Editor |
| **Check if email exists in auth** | `SELECT * FROM auth.users WHERE email = '...'` |
| **Check if email exists in users** | `SELECT * FROM users WHERE email = '...'` |
| **Delete from auth only** | `DELETE FROM auth.users WHERE email = '...'` |
| **Delete from users only** | `DELETE FROM users WHERE email = '...'` |

---

## Summary

1. ✅ **Problem identified**: Email exists in `auth.users` but you only deleted from `users`
2. ✅ **Solution provided**: SQL to delete from both tables
3. ✅ **Error handling improved**: Now shows user-friendly messages
4. ✅ **Documentation created**: DELETE_USER_PROPERLY.md with detailed explanation

**Next Step**: Run the SQL command above to delete `arslanmunawar1311@gmail.com` from `auth.users`, then try registering again!

---

## Need Help?

If you still get "email already in use" after running the SQL:

1. Verify deletion worked:
   ```sql
   SELECT * FROM auth.users WHERE email = 'arslanmunawar1311@gmail.com';
   ```
   Should return **no rows**.

2. Check for typos in the email address

3. Clear the app cache and try again

4. Make sure you're using the exact same email when registering

The email should work after properly deleting from both tables!
