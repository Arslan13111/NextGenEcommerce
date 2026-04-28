# NextGen Ecommerce

A full-featured Android e-commerce app for fashion retail, with AI-powered virtual try-on, AR live camera try-on, admin dashboard, and Pakistani payment gateway integration.

---

## Latest Update: Performance & Branding Optimization (v3.0 Final)

### **Title: Application Branding & Theme System Optimization**

**Description:**
This update establishes a cohesive visual identity and ensures a seamless user experience across different system settings and device types.

#### **1. Branding & Logo Integration**
*   **Universal Logo Update:** Replaced all placeholder branding with the official `logo.png` across Splash, Login, Register, and About screens.
*   **Circular Identity:** Applied a precise circular crop and high-quality scaling to the logo on authentication screens.
*   **Custom Home Header:** Integrated a specialized `home_logo.png` into the Home screen's top bar, optimized for a professional look.
*   **Launcher Refresh:** Updated the Android system launcher and round icons to match the new branding.

#### **2. Intelligent Theme System**
*   **System Theme Synchronization:** The app now automatically toggles between Light and Dark modes in real-time based on the user's Android system settings.
*   **Enhanced Settings Control:** Added a "Follow System" toggle in the settings menu for automated or manual appearance management.

#### **3. Performance & Structural Stability**
*   **Layout Stabilization:** Moved the Navigation Bar to a persistent overlay to eliminate "layout jumping" during screen transitions.
*   **Recomposition Reduction:** Implemented `derivedStateOf` in critical UI components (`HomeScreen`, `ProductDetailScreen`) to improve scrolling performance and reduce CPU usage.
*   **Build Optimization:** Enabled R8 code minification and resource shrinking for a smaller, faster final APK.
*   **Smooth Transitions:** Standardized 300ms cross-fade animations for all screen navigation.

---

## Features

### Shopping
- Browse products by category (Clothing, Shoes, Accessories, Bags, Jewelry, Furniture, Electronics, Home Decor)
- Product search and filtering
- Color variant selection — each color is explicitly paired with its own product image
- Size selection
- Wishlist
- Shopping cart with quantity management

### Virtual Try-On
- **AI Try-On** — Upload a photo and try any product virtually using the IDM-VTON diffusion model (via Hugging Face / RapidAPI)
- **Live AR Try-On** — Real-time camera try-on powered by Snap Camera Kit and ARCore

### Orders & Payments
- **Safepay** — Pakistani payment gateway with OTP verification
- **Cash on Delivery**
- Order history, order detail, and status tracking
- Return policy screen

### User Account
- Email + Google sign-in via Supabase Auth
- Profile management, address book, change password
- Notifications for order status updates

### Admin Panel
- Dashboard with revenue overview, order stats, and stock alerts
- Product CRUD — add, edit, delete products with image upload
- **Color picker** — tap any color swatch to open a 30-preset fashion color panel with custom hex input; changes sync to Supabase instantly
- Order management — view all orders, update status (Pending → Confirmed → Processing → Shipped → Delivered, etc.)

---

## Tech Stack

### Android App
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (BOM 2023.10.01) |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.48 |
| Navigation | Navigation Compose 2.7.6 |
| Backend | Supabase 2.0.4 (Auth, Database, Storage, Realtime) |
| HTTP | Ktor Client 2.3.7 / Retrofit 2.9.0 |
| Local DB | Room 2.6.1 |
| Image Loading | Coil 2.5.0 / Glide 4.16.0 |
| AR | ARCore 1.41.0 + Sceneform 1.17.1 |
| Live AR | Snap Camera Kit 1.46.0 |
| AI/ML | ML Kit (image labeling, pose detection) + TensorFlow Lite 2.14.0 |
| Camera | CameraX 1.3.1 |
| Animations | Lottie 6.3.0 |
| Preferences | DataStore 1.0.0 |
| Paging | Paging 3 2.3.1 |

**Build config:** `compileSdk 34`, `minSdk 26`, `targetSdk 34`, JVM 17

### Backend (Node.js)
| Layer | Technology |
|---|---|
| Framework | Express 4.18.2 |
| Database | Supabase JS SDK 2.90.1 |
| HTTP | Axios 1.6.8 |
| Dev server | Nodemon 3.0.1 |

---

## Architecture

```
app/src/main/java/com/example/nextgenecommerce/
├── presentation/
│   ├── screens/
│   │   ├── auth/          (Login, Register, Splash)
│   │   ├── home/          (Home, AllProducts, Search)
│   │   ├── product/       (ProductList, ProductDetail)
│   │   ├── cart/          (Cart, Checkout, OrderSuccess)
│   │   ├── orders/        (Orders, OrderDetail)
│   │   ├── profile/       (Profile, Account, Settings, Addresses)
│   │   ├── wishlist/
│   │   ├── tryon/         (TryOn AI, Live AR)
│   │   ├── payment/       (Safepay, OTP)
│   │   └── admin/         (Dashboard, AddProduct, ColorPickerDialog)
│   ├── viewmodel/         (9 ViewModels)
│   ├── navigation/
│   ├── components/
│   └── theme/
├── data/
│   ├── models/            (ProductEntity, Order, User, AddressEntity)
│   ├── local/             (Room DAO + Database)
│   ├── remote/
│   ├── repository/        (8 repositories)
│   └── config/            (SupabaseConfig)
├── di/                    (Hilt modules)
└── util/
```

**Data flow:** Supabase (source of truth) → Room (local cache) → Repository → ViewModel → Compose UI

---

## Data Models

### ProductEntity
```
id, name, description, price, originalPrice, brand, category, subCategory
images: List<String>           — all product image URLs (one per color)
colors: List<String>           — color names in same order as images
colorImages: Map<String,String>— explicit color → imageUrl mapping
sizes: List<String>
stock, inStock, isFeatured, isNew
lensId                         — Snap Camera Kit lens for live AR
arModelUrl                     — 3D model for AR view
rating, reviewCount, tags
```

### Order
```
id, orderNumber, userId
items: List<OrderItem>          — product snapshot + qty, size, color
subtotal, tax, shipping, total  (PKR)
status: OrderStatus             — PENDING | CONFIRMED | PROCESSING | SHIPPED | OUT_FOR_DELIVERY | DELIVERED | CANCELLED | RETURNED
paymentMethod                   — SAFEPAY | CASH_ON_DELIVERY
paymentStatus                   — PENDING | COMPLETED | FAILED | REFUNDED
shippingAddress, trackingNumber, estimatedDelivery
```

---

## Supabase Setup

1. Create a Supabase project at [supabase.com](https://supabase.com)
2. Add your credentials to `app/src/main/java/com/example/nextgenecommerce/data/config/SupabaseConfig.kt`:
   ```kotlin
   const val SUPABASE_URL = "https://your-project.supabase.co"
   const val SUPABASE_KEY = "your-anon-key"
   ```
3. Create tables: `users`, `products`, `orders`, `addresses`, `notifications`
4. Enable Row Level Security (RLS) on all tables
5. Create storage buckets: `product-images`, `profiles`
6. Enable Auth providers: Email, Google (optional)
7. See `SUPABASE_MIGRATION_GUIDE.md` for full SQL scripts and RLS policies

---

## Backend Setup

The Node.js server bridges the AI try-on API and handles email confirmation.

```bash
cd backend
npm install

# Create .env
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-service-role-key
RAPIDAPI_KEY=your-rapidapi-key       # for IDM-VTON try-on

npm run dev    # development (nodemon)
npm start      # production
```

**Endpoints:**
- `POST /api/try-on-diffusion` — runs AI virtual try-on (user image + product image → output image uploaded to Supabase Storage)
- `GET /auth/confirm` — email confirmation redirect page

---

## Building the App

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

Minimum Android version: **Android 8.0 (API 26)** — required by ARCore.

---

## Admin Access

Admin users are identified by email. Add admin emails to the auth check in `AuthViewModel`. Current admin accounts:
- `arslanmunawar1311@gmail.com`
- `huzaifanadeem1192@gmail.com`

---

## Color Picker

Products support per-color image variants. In the admin panel:
- The **Products tab** shows tappable color dots on each product card — tap any dot to open the color picker
- The **Add/Edit Product screen** has clickable color swatches in the variant form
- The color picker provides 30 preset fashion colors and a custom hex input field
- Changes update both `colors[]` and `colorImages{}` in Supabase and sync to Room

---

## Project Documentation

| File | Contents |
|---|---|
| `SUPABASE_MIGRATION_GUIDE.md` | Full Supabase setup, SQL schemas, RLS policies |
| `BUILD_FIX_SUMMARY.md` | Common build issues and fixes |
| `MIGRATION_SUMMARY.md` | Firebase → Supabase migration notes |
| `ERROR_HANDLING_IMPROVEMENTS.md` | Error handling patterns used |
| `USER_DELETION_SOLUTION.md` | Account deletion flow |
