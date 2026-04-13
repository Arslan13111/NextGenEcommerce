# Firebase Firestore & Storage Setup Instructions

## Overview
Your profile picture upload feature is now implemented! The uploaded images will be stored in **Firebase Storage** (not Firestore). Firestore will only store the download URL of the image.

## Firebase Console Setup Steps

### 1. Enable Firebase Storage

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **NextGenEcommerce**
3. In the left sidebar, click on **"Build"** → **"Storage"**
4. Click **"Get Started"**
5. Choose **"Start in test mode"** (for development) or **"Start in production mode"** (for production)
6. Click **"Next"** and then **"Done"**

### 2. Configure Storage Security Rules

After enabling Storage, you need to set up security rules:

1. In the Storage section, click on the **"Rules"** tab
2. Replace the default rules with the following:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Allow users to read/write their own profile pictures
    match /users/{userId}/profile/{allPaths=**} {
      allow read: if true; // Anyone can view profile pictures
      allow write: if request.auth != null && request.auth.uid == userId;
      allow delete: if request.auth != null && request.auth.uid == userId;
    }

    // Allow authenticated users to upload images to other paths
    match /{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

3. Click **"Publish"** to save the rules

### 3. Configure Firestore Security Rules (for user data)

1. In the left sidebar, click on **"Build"** → **"Firestore Database"**
2. Click on the **"Rules"** tab
3. Update your rules to allow profile updates:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - users can read their own data and update their profile
    match /users/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null && request.auth.uid == userId;
      allow update: if request.auth != null && request.auth.uid == userId;
      allow delete: if request.auth != null && request.auth.uid == userId;
    }

    // Add other collection rules as needed
  }
}
```

4. Click **"Publish"** to save the rules

### 4. Update Storage CORS (Optional - for Web)

If you plan to access images from a web application, you may need to configure CORS:

1. Install Google Cloud SDK (if not already installed)
2. Create a file named `cors.json` with:

```json
[
  {
    "origin": ["*"],
    "method": ["GET"],
    "maxAgeSeconds": 3600
  }
]
```

3. Run the following command:
```bash
gsutil cors set cors.json gs://YOUR_BUCKET_NAME.appspot.com
```

## How the Profile Picture Upload Works

### Storage Structure
Images are stored in Firebase Storage with the following path structure:
```
users/
  └── {userId}/
      └── profile/
          └── profile_{userId}_{timestamp}.jpg
```

### Firestore Structure
User documents in Firestore have the following structure:
```json
{
  "id": "user123",
  "email": "user@example.com",
  "name": "John Doe",
  "phone": "",
  "profileImageUrl": "https://firebasestorage.googleapis.com/.../profile_user123_1234567890.jpg",
  "createdAt": 1234567890,
  "role": "CUSTOMER"
}
```

## Testing the Feature

1. **Run the app** and navigate to the Profile screen
2. **Tap on the profile picture** (circular area with camera icon)
3. **Select "Choose from Gallery"** to pick an image
4. The image will be uploaded to Firebase Storage
5. Once uploaded, the `profileImageUrl` field in Firestore will be updated
6. The profile picture will be displayed in the app

## Verifying in Firebase Console

### Check Storage Upload:
1. Go to Firebase Console → Storage
2. Navigate to `users/{your-user-id}/profile/`
3. You should see the uploaded image file

### Check Firestore Update:
1. Go to Firebase Console → Firestore Database
2. Open the `users` collection
3. Find your user document
4. Verify that `profileImageUrl` field contains the Firebase Storage URL

## Security Best Practices

1. **Production Rules**: Before going to production, update your Storage rules to be more restrictive
2. **File Size Limits**: Consider adding file size validation in the app
3. **File Type Validation**: Ensure only images are uploaded
4. **Image Compression**: Compress images before uploading to save storage costs

## Troubleshooting

### Issue: Upload fails with "Permission denied"
**Solution**: Check that:
- Firebase Storage is enabled
- Storage security rules are published
- User is authenticated
- The upload path matches the security rules

### Issue: Image URL not saving to Firestore
**Solution**: Check that:
- Firestore security rules allow updates to user documents
- User is authenticated
- Network connection is stable

### Issue: Images not loading in the app
**Solution**: Check that:
- Storage security rules allow read access
- The URL is correct in Firestore
- Coil library is properly configured in build.gradle

## Cost Considerations

Firebase Storage pricing:
- **Storage**: $0.026 per GB/month
- **Downloads**: $0.12 per GB
- **Uploads**: $0.12 per GB

For a typical profile picture (500KB compressed):
- 1000 users = ~488 MB storage = ~$0.01/month
- Very cost-effective for profile pictures!

## Next Steps

1. Enable Firebase Storage in console (follow steps above)
2. Set up security rules
3. Test the feature in your app
4. Consider adding image compression for better performance
5. Add loading indicators for better UX

---

**Note**: Make sure your `google-services.json` file is up to date and includes Storage configuration.
