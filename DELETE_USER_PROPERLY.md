# How to Properly Delete a User in Supabase

## The Problem

When you deleted the user, you only deleted from **ONE** of these tables:
- ❌ **auth.users** (still exists) ← This is the authentication table
- ✅ **users** (deleted) ← This is your custom table

**Result**: Supabase Auth still thinks the email is registered!

---

## Understanding Supabase Tables

Supabase has **TWO** separate tables for users:

### 1. `auth.users` (Authentication Table)
- **Managed by**: Supabase Auth
- **Location**: Authentication → Users (in dashboard)
- **Contains**: Email, password hash, email confirmation status
- **Used for**: Login, authentication, email confirmation

### 2. `users` (Your Custom Table)
- **Managed by**: You (via Table Editor)
- **Location**: Table Editor → users
- **Contains**: Profile data (name, phone, profile image, etc.)
- **Used for**: Storing additional user information

**They are SEPARATE!** Deleting from one doesn't delete from the other.

---

## How to Properly Delete a User

### Option 1: Using Supabase Dashboard (Easiest)

#### Step 1: Delete from Authentication
1. Go to **Authentication** → **Users**
2. Find user: `arslanmunawar1311@gmail.com`
3. Click the **trash icon** or **⋮** menu
4. Click **"Delete User"**
5. Confirm deletion

#### Step 2: Delete from Users Table
1. Go to **Table Editor** → **users** table
2. Find the row with email: `arslanmunawar1311@gmail.com`
3. Click the row → Click **Delete**
4. Confirm deletion

**Now you can register with that email again!**

---

### Option 2: Using SQL (Fastest)

Run this in **SQL Editor**:

```sql
-- Delete from Authentication table
DELETE FROM auth.users WHERE email = 'arslanmunawar1311@gmail.com';

-- Delete from your users table
DELETE FROM users WHERE email = 'arslanmunawar1311@gmail.com';
```

**Done!** Email is now completely freed up.

---

## For Your Specific Case

To delete **arslanmunawar1311@gmail.com** right now:

### Quick Fix (Run in SQL Editor):
```sql
-- Delete this specific user from auth
DELETE FROM auth.users WHERE email = 'arslanmunawar1311@gmail.com';

-- Delete from users table
DELETE FROM users WHERE email = 'arslanmunawar1311@gmail.com';

-- Verify deletion
SELECT * FROM auth.users WHERE email = 'arslanmunawar1311@gmail.com';  -- Should return empty
SELECT * FROM users WHERE email = 'arslanmunawar1311@gmail.com';  -- Should return empty
```

After running this, you can register with `arslanmunawar1311@gmail.com` again!

---

## Why This Happens

### Registration Flow:
1. User registers
2. ✅ Supabase creates entry in `auth.users`
3. ✅ Your app creates entry in `users` table
4. Both tables have the email

### Wrong Deletion:
1. You delete from Table Editor (`users` table)
2. ❌ `auth.users` still has the email
3. You try to register again
4. ❌ Supabase says "email already exists" (in auth.users)

### Correct Deletion:
1. Delete from **Authentication** → Users
2. Delete from **Table Editor** → users
3. Both tables cleared
4. ✅ Can register again!

---

## Preventing This in the Future

### Create a SQL Function to Delete User Completely

Add this function to your Supabase project:

```sql
-- Create a function to delete user from both tables
CREATE OR REPLACE FUNCTION delete_user_completely(user_email TEXT)
RETURNS void AS $$
BEGIN
    -- Delete from users table
    DELETE FROM users WHERE email = user_email;

    -- Delete from auth.users
    DELETE FROM auth.users WHERE email = user_email;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Usage**:
```sql
-- Delete a user completely
SELECT delete_user_completely('email@example.com');
```

---

## Testing After Deletion

After deleting the user, verify it's completely gone:

```sql
-- Check auth.users
SELECT id, email, created_at FROM auth.users WHERE email = 'arslanmunawar1311@gmail.com';

-- Check users table
SELECT id, email, name FROM users WHERE email = 'arslanmunawar1311@gmail.com';
```

Both queries should return **0 rows**.

---

## Common Questions

### Q: Why are there two tables?
**A**: Supabase Auth manages authentication (`auth.users`), you manage profile data (`users`). This separation is by design for security and flexibility.

### Q: Does the trigger create users in both tables?
**A**: The trigger only creates in the `users` table. `auth.users` is created by Supabase Auth during signup.

### Q: What if I only delete from auth.users?
**A**: You'll have orphaned data in the `users` table. The user can't login, but their profile data remains.

### Q: What if I only delete from users table?
**A**: The email is still "taken" in auth.users. User can't register with that email, but has no profile data.

---

## Best Practices

### 1. Always Delete from Both Tables
When removing a user, delete from:
- ✅ `auth.users` (Authentication → Users)
- ✅ `users` (Table Editor → users)

### 2. Use Cascading Delete (Optional)
Add to your database schema:

```sql
-- This will auto-delete from users table when deleted from auth.users
CREATE OR REPLACE FUNCTION handle_user_delete()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM users WHERE id = OLD.id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_deleted
    BEFORE DELETE ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION handle_user_delete();
```

Now deleting from Authentication will auto-delete from users table!

### 3. Use the Dashboard's Delete User Feature
The **Authentication → Users → Delete User** button should handle both tables automatically.

---

## Quick Reference

| Task | Location | SQL |
|------|----------|-----|
| Delete from Auth | Authentication → Users | `DELETE FROM auth.users WHERE email = '...'` |
| Delete from Profile | Table Editor → users | `DELETE FROM users WHERE email = '...'` |
| Delete Completely | SQL Editor | Run both DELETE statements above |

---

## Summary

**Your Issue**: Email `arslanmunawar1311@gmail.com` still exists in `auth.users`

**Quick Fix**:
1. Go to Supabase Dashboard
2. Open SQL Editor
3. Run:
   ```sql
   DELETE FROM auth.users WHERE email = 'arslanmunawar1311@gmail.com';
   DELETE FROM users WHERE email = 'arslanmunawar1311@gmail.com';
   ```
4. Try registering again ✅

**Result**: Email is completely freed up and you can register again!

---

## Need Help?

If you still get "email already in use" after running the SQL:
1. Check if user exists: `SELECT * FROM auth.users;`
2. Look for any user with that email
3. Make sure both DELETE statements ran successfully
4. Try closing and reopening the app

The email should work after properly deleting from both tables! 🎉
