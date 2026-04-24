const docx = require("docx");
const fs = require("fs");

const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  WidthType, AlignmentType, HeadingLevel, BorderStyle, ShadingType,
  TableLayoutType, VerticalAlign, PageBreak
} = docx;

// ── Helpers ──────────────────────────────────────────────────────────

function heading(text, level = HeadingLevel.HEADING_1) {
  return new Paragraph({ heading: level, spacing: { before: 300, after: 150 }, children: [new TextRun({ text, bold: true })] });
}

function para(text, opts = {}) {
  return new Paragraph({ spacing: { after: 100 }, children: [new TextRun({ text, ...opts })] });
}

function bullet(text, level = 0) {
  return new Paragraph({ bullet: { level }, spacing: { after: 60 }, children: [new TextRun({ text })] });
}

const thinBorder = { style: BorderStyle.SINGLE, size: 1, color: "AAAAAA" };
const cellBorders = { top: thinBorder, bottom: thinBorder, left: thinBorder, right: thinBorder };

function headerCell(text, width) {
  return new TableCell({
    width: { size: width, type: WidthType.PERCENTAGE },
    shading: { type: ShadingType.SOLID, color: "1F4E79" },
    borders: cellBorders,
    verticalAlign: VerticalAlign.CENTER,
    children: [new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 60, after: 60 }, children: [new TextRun({ text, bold: true, color: "FFFFFF", size: 20 })] })]
  });
}

function cell(text, width, opts = {}) {
  return new TableCell({
    width: { size: width, type: WidthType.PERCENTAGE },
    borders: cellBorders,
    verticalAlign: VerticalAlign.TOP,
    children: [new Paragraph({ spacing: { before: 40, after: 40 }, children: [new TextRun({ text, size: 19, ...opts })] })]
  });
}

function multiCell(lines, width) {
  return new TableCell({
    width: { size: width, type: WidthType.PERCENTAGE },
    borders: cellBorders,
    verticalAlign: VerticalAlign.TOP,
    children: lines.map(l => new Paragraph({ spacing: { before: 20, after: 20 }, children: [new TextRun({ text: l, size: 19 })] }))
  });
}

function makeTable(headers, rows, widths) {
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    layout: TableLayoutType.FIXED,
    rows: [
      new TableRow({ children: headers.map((h, i) => headerCell(h, widths[i])), tableHeader: true }),
      ...rows.map(r => new TableRow({
        children: r.map((c, i) => {
          if (Array.isArray(c)) return multiCell(c, widths[i]);
          return cell(c, widths[i]);
        })
      }))
    ]
  });
}

// ── Data ─────────────────────────────────────────────────────────────

const epics = [
  // ─── EPIC 1 ───
  {
    id: "EP-01",
    name: "User Authentication & Account Management",
    description: "Complete user registration, login (email & Google OAuth), password management, profile management, and admin authentication via Supabase Auth.",
    useCases: [
      { id: "UC-01.01", name: "Email Registration", actor: "Guest User", precondition: "User is not logged in", flow: "User enters name, email, password → App calls Supabase signUpWith(Email) → User record created in auth.users → Profile row inserted into users table → Confirmation email sent → Success message shown", postcondition: "User account created, email confirmation pending" },
      { id: "UC-01.02", name: "Email Login", actor: "Registered User", precondition: "User has confirmed email", flow: "User enters email + password → Supabase signInWith(Email) → Session token received → User profile fetched from users table → Navigate to Home", postcondition: "User is authenticated and on Home screen" },
      { id: "UC-01.03", name: "Google Sign-In", actor: "Guest/Registered User", precondition: "Google Play Services available", flow: "User taps Google button → Google OAuth flow → ID token received → Supabase signInWith(IDToken, Google) → Check if users row exists → Create if new → Navigate to Home", postcondition: "User authenticated via Google" },
      { id: "UC-01.04", name: "Password Reset", actor: "Registered User", precondition: "User has an account", flow: "User taps Forgot Password → Enters email → Supabase resetPasswordForEmail() → Reset email sent → User follows link → Sets new password", postcondition: "Password updated" },
      { id: "UC-01.05", name: "Change Password", actor: "Authenticated User", precondition: "User is logged in", flow: "User navigates to Settings → Change Password → Enters new password → Supabase modifyUser(password) → Success confirmation", postcondition: "Password changed" },
      { id: "UC-01.06", name: "Update Profile", actor: "Authenticated User", precondition: "User is logged in", flow: "User navigates to Profile → Edits name/phone/image → Supabase updates users table → Local state refreshed", postcondition: "Profile updated in Supabase" },
      { id: "UC-01.07", name: "Admin Login", actor: "Admin User", precondition: "User has admin role or matching UUID", flow: "Admin enters email + password on Admin Login screen → Supabase auth → Verify role='admin' or ID matches hardcoded UUID → Grant access to AdminDashboard", postcondition: "Admin authenticated and on Dashboard" },
      { id: "UC-01.08", name: "Logout", actor: "Authenticated User", precondition: "User is logged in", flow: "User taps Logout → Supabase signOut() → Clear local session → Navigate to Login", postcondition: "User session terminated" },
    ],
    testCases: [
      { id: "TC-01.01", title: "Register with valid email", steps: "Enter name, valid email, password (8+ chars) → Tap Register", expected: "Account created, success message shown, confirmation email sent" },
      { id: "TC-01.02", title: "Register with duplicate email", steps: "Enter already-registered email → Tap Register", expected: "Error: 'This email is already registered. Please login instead.'" },
      { id: "TC-01.03", title: "Login with valid credentials", steps: "Enter correct email/password → Tap Login", expected: "Navigates to Home screen, user data loaded" },
      { id: "TC-01.04", title: "Login with wrong password", steps: "Enter valid email, wrong password → Tap Login", expected: "Error message displayed, user stays on Login screen" },
      { id: "TC-01.05", title: "Google Sign-In new user", steps: "Tap Google Sign-In → Complete OAuth → First-time user", expected: "New profile created in users table, navigates to Home" },
      { id: "TC-01.06", title: "Google Sign-In existing user", steps: "Tap Google Sign-In → Existing account", expected: "Existing profile loaded, navigates to Home" },
      { id: "TC-01.07", title: "Password reset email", steps: "Tap Forgot Password → Enter valid email → Submit", expected: "Reset email sent, confirmation message shown" },
      { id: "TC-01.08", title: "Change password", steps: "Navigate to Settings → Change Password → Enter new password → Submit", expected: "Password updated, success confirmation" },
      { id: "TC-01.09", title: "Admin login with non-admin account", steps: "Enter regular user credentials on Admin Login", expected: "Error: 'Access denied. Admin privileges required.', user signed out" },
      { id: "TC-01.10", title: "Admin login with admin account", steps: "Enter admin credentials on Admin Login", expected: "Navigates to Admin Dashboard" },
      { id: "TC-01.11", title: "Logout", steps: "Tap Logout button", expected: "Session cleared, navigates to Login screen" },
      { id: "TC-01.12", title: "Update profile name", steps: "Navigate to Profile → Change name → Save", expected: "Name updated in Supabase users table" },
    ]
  },

  // ─── EPIC 2 ───
  {
    id: "EP-02",
    name: "Product Catalog & Browsing",
    description: "Browse, search, filter, and view product details. Products are synced from Supabase to a local Room database for offline access. Supports categories, featured items, and new arrivals.",
    useCases: [
      { id: "UC-02.01", name: "View Home Screen Products", actor: "User", precondition: "App launched, products synced", flow: "Home screen loads → ProductViewModel fetches allProducts from Room → Products displayed in a 2-column grid with ProductCard composable showing image, name, price, rating", postcondition: "User sees product catalog" },
      { id: "UC-02.02", name: "Sync Products from Supabase", actor: "User/System", precondition: "Internet available", flow: "User taps Refresh or app startup → syncProductsFromSupabase() → Postgrest SELECT * from products → Convert SupabaseProduct → ProductEntity → Delete all local, insert fresh → Room emits updated Flow", postcondition: "Local Room DB has latest products from Supabase" },
      { id: "UC-02.03", name: "View Product Detail", actor: "User", precondition: "Products loaded", flow: "User taps ProductCard → Navigate to ProductDetail screen → Load product by ID from Room → Show images (carousel), name, brand, price, description, sizes, colors, rating, reviews, stock, Try-On button", postcondition: "User views full product information" },
      { id: "UC-02.04", name: "Browse by Category", actor: "User", precondition: "Products loaded", flow: "User selects category → Navigate to ProductList screen → Filter by ProductCategory enum (CLOTHING, SHOES, ACCESSORIES, etc.) → Display filtered grid", postcondition: "User sees category-filtered products" },
      { id: "UC-02.05", name: "Search Products", actor: "User", precondition: "Products loaded", flow: "User navigates to Search screen → Types query → Room searchProducts(query) matches against name/description → Results displayed", postcondition: "Matching products shown" },
      { id: "UC-02.06", name: "View Featured Products", actor: "User", precondition: "Products loaded", flow: "Home screen section → getFeaturedProducts() from Room where isFeatured=true → Display featured section", postcondition: "Featured products highlighted" },
      { id: "UC-02.07", name: "View New Arrivals", actor: "User", precondition: "Products loaded", flow: "Home screen section → getNewProducts() from Room where isNew=true → Display new arrivals section", postcondition: "New products highlighted" },
    ],
    testCases: [
      { id: "TC-02.01", title: "Home screen loads products", steps: "Launch app → Wait for sync", expected: "Product grid displayed with images, names, prices" },
      { id: "TC-02.02", title: "Refresh products from Supabase", steps: "Tap Refresh icon on Home", expected: "Products re-fetched from Supabase, grid updated" },
      { id: "TC-02.03", title: "View product detail", steps: "Tap any product card", expected: "Navigate to detail screen with full product info, image carousel, sizes, colors" },
      { id: "TC-02.04", title: "Browse CLOTHING category", steps: "Select CLOTHING category", expected: "Only clothing products displayed" },
      { id: "TC-02.05", title: "Search for 'jacket'", steps: "Go to Search → Type 'jacket'", expected: "Products with 'jacket' in name/description shown" },
      { id: "TC-02.06", title: "Search with no results", steps: "Search for 'xyznonexistent'", expected: "Empty state or 'No products found' message" },
      { id: "TC-02.07", title: "Offline product access", steps: "Sync products → Turn off internet → Browse", expected: "Products still visible from Room cache" },
      { id: "TC-02.08", title: "Product image carousel", steps: "Open product with multiple images → Swipe", expected: "Images cycle through carousel" },
      { id: "TC-02.09", title: "Featured products section", steps: "Check Home screen", expected: "Featured products section shows isFeatured=true products" },
    ]
  },

  // ─── EPIC 3 ───
  {
    id: "EP-03",
    name: "Shopping Cart Management",
    description: "Add products to cart with size/color selection, update quantities, remove items, and view cart summary with totals.",
    useCases: [
      { id: "UC-03.01", name: "Add Product to Cart", actor: "User", precondition: "Viewing product detail", flow: "User selects size + color → Taps Add to Cart → CartItem created with productId, name, image, price, quantity, selectedSize, selectedColor → Inserted into Room cart_items table → Cart badge updates", postcondition: "Product added to cart" },
      { id: "UC-03.02", name: "View Cart", actor: "User", precondition: "Items in cart", flow: "User taps Cart tab → CartScreen loads → CartViewModel fetches cart items from Room → Display CartItemCards with image, name, price, size, color, quantity controls, subtotal", postcondition: "Cart contents displayed with totals" },
      { id: "UC-03.03", name: "Update Cart Item Quantity", actor: "User", precondition: "Items in cart", flow: "User taps +/- buttons on CartItemCard → CartDao updates quantity → UI refreshes → Total recalculated", postcondition: "Quantity updated" },
      { id: "UC-03.04", name: "Remove Cart Item", actor: "User", precondition: "Items in cart", flow: "User swipes or taps delete on cart item → CartDao deletes item → Cart refreshed → Badge count updated", postcondition: "Item removed from cart" },
      { id: "UC-03.05", name: "Cart Sync with Supabase", actor: "Authenticated User", precondition: "User logged in", flow: "Cart items synced to Supabase cart table via SupabaseCartItem serialization → Available across devices", postcondition: "Cart persisted in cloud" },
    ],
    testCases: [
      { id: "TC-03.01", title: "Add item to cart", steps: "Open product → Select size M, color Black → Tap Add to Cart", expected: "Item appears in cart, badge shows count 1" },
      { id: "TC-03.02", title: "Add same product different size", steps: "Add product in size M, then add same product in size L", expected: "Two separate cart items shown" },
      { id: "TC-03.03", title: "Increase quantity", steps: "In cart, tap + button on item", expected: "Quantity increments, subtotal updates" },
      { id: "TC-03.04", title: "Decrease quantity to 1", steps: "Item with qty 2 → Tap - button", expected: "Quantity decrements to 1" },
      { id: "TC-03.05", title: "Remove item from cart", steps: "Swipe/tap delete on cart item", expected: "Item removed, totals recalculated" },
      { id: "TC-03.06", title: "Empty cart state", steps: "Remove all items from cart", expected: "Empty cart message/illustration shown" },
      { id: "TC-03.07", title: "Cart badge count", steps: "Add 3 different items", expected: "Cart tab badge shows 3" },
    ]
  },

  // ─── EPIC 4 ───
  {
    id: "EP-04",
    name: "Wishlist Management",
    description: "Save products to a wishlist for later. Add/remove items, view wishlist, and move items to cart.",
    useCases: [
      { id: "UC-04.01", name: "Add to Wishlist", actor: "User", precondition: "Viewing product", flow: "User taps heart/favorite icon → WishlistItem created (productId, name, image, price) → Inserted into Room wishlist_items → Heart icon fills/animates", postcondition: "Product saved to wishlist" },
      { id: "UC-04.02", name: "View Wishlist", actor: "User", precondition: "Items in wishlist", flow: "User navigates to Wishlist screen → WishlistViewModel loads from Room → Display product cards with remove option", postcondition: "Wishlist displayed" },
      { id: "UC-04.03", name: "Remove from Wishlist", actor: "User", precondition: "Item in wishlist", flow: "User taps remove/unfavorite → WishlistDao deletes → UI refreshes → Badge count updates", postcondition: "Item removed from wishlist" },
    ],
    testCases: [
      { id: "TC-04.01", title: "Add product to wishlist", steps: "Tap heart icon on product card", expected: "Heart fills, product appears in Wishlist screen" },
      { id: "TC-04.02", title: "Remove from wishlist", steps: "Tap filled heart icon or remove button", expected: "Product removed from wishlist" },
      { id: "TC-04.03", title: "Wishlist badge count", steps: "Add 2 items to wishlist", expected: "Wishlist icon shows badge count 2" },
      { id: "TC-04.04", title: "View wishlist screen", steps: "Navigate to Wishlist tab", expected: "All wishlisted products displayed" },
      { id: "TC-04.05", title: "Empty wishlist", steps: "Remove all wishlist items", expected: "Empty state message shown" },
    ]
  },

  // ─── EPIC 5 ───
  {
    id: "EP-05",
    name: "Order Management & Checkout",
    description: "Place orders from cart, process payment, track order status through its lifecycle (Pending → Confirmed → Processing → Shipped → Delivered), and view order history.",
    useCases: [
      { id: "UC-05.01", name: "Checkout", actor: "Authenticated User", precondition: "Items in cart, user logged in", flow: "User taps Checkout from Cart → CheckoutScreen loads → Enter/select shipping address → Select payment method → Review order summary (items, subtotal, tax, shipping, total) → Confirm order", postcondition: "Order placed" },
      { id: "UC-05.02", name: "Place Order", actor: "Authenticated User", precondition: "Checkout completed", flow: "User confirms → CreateOrderRequest sent (userId, items, address, paymentMethod) → Backend creates order with status PENDING, paymentStatus COMPLETED → Order saved to Room orders table → Navigate to success/orders", postcondition: "Order created with PENDING status" },
      { id: "UC-05.03", name: "View Order History", actor: "Authenticated User", precondition: "User has placed orders", flow: "User navigates to Orders tab → OrderViewModel loads orders from Room → Display list with order ID, status, total, date", postcondition: "Order history displayed" },
      { id: "UC-05.04", name: "View Order Detail", actor: "Authenticated User", precondition: "Orders exist", flow: "User taps order → OrderDetailScreen → Shows items, status timeline, tracking number, shipping address, payment info, estimated delivery", postcondition: "Full order details shown" },
      { id: "UC-05.05", name: "Track Order Status", actor: "Authenticated User", precondition: "Order placed", flow: "Order progresses: PENDING → CONFIRMED → PROCESSING → SHIPPED → OUT_FOR_DELIVERY → DELIVERED. Each status change reflected in OrderTracking with timestamp and optional location", postcondition: "User sees current order status" },
    ],
    testCases: [
      { id: "TC-05.01", title: "Navigate to checkout", steps: "Add items to cart → Tap Checkout", expected: "Checkout screen loads with cart items summary" },
      { id: "TC-05.02", title: "Place order successfully", steps: "Fill address → Select payment → Confirm", expected: "Order created, success screen shown, cart cleared" },
      { id: "TC-05.03", title: "View order history", steps: "Navigate to Orders tab", expected: "List of past orders with status and totals" },
      { id: "TC-05.04", title: "View order detail", steps: "Tap on an order in history", expected: "Full order details with items, address, status" },
      { id: "TC-05.05", title: "Order status progression", steps: "Check order at different stages", expected: "Status shows correct stage (PENDING/PROCESSING/SHIPPED/DELIVERED)" },
      { id: "TC-05.06", title: "Checkout with empty cart", steps: "Try to checkout with no items", expected: "Checkout button disabled or error shown" },
    ]
  },

  // ─── EPIC 6 ───
  {
    id: "EP-06",
    name: "AI Virtual Try-On",
    description: "The flagship feature. Users can virtually try on clothing products using AI diffusion models. Three approaches: RapidAPI Try-On Diffusion (primary), Hugging Face IDM-VTON (fallback), and Snap Camera Kit for live AR.",
    useCases: [
      { id: "UC-06.01", name: "Navigate to Try-On", actor: "User", precondition: "Viewing product detail", flow: "User taps 'Try On' button on ProductDetail → Navigate to TryOnScreen with productId and imageIndex → Product image URL loaded from products.images[]", postcondition: "Try-On screen shown with product info" },
      { id: "UC-06.02", name: "Select Avatar from Profile Photo", actor: "Authenticated User", precondition: "User has profileImageUrl set", flow: "User taps 'Choose Your Photo' → Dialog shows → Selects 'Use Profile Photo' → TryOnViewModel downloads profile image → Avatar bitmap set → Source shown as 'Profile Photo'", postcondition: "Avatar loaded from profile" },
      { id: "UC-06.03", name: "Select Avatar from Gallery", actor: "User", precondition: "On Try-On screen", flow: "User taps 'Choose Your Photo' → Selects 'Upload from Gallery' → Image picker opens → User selects image → TryOnViewModel converts URI to bitmap → Avatar set", postcondition: "Avatar loaded from gallery" },
      { id: "UC-06.04", name: "Capture Avatar from Camera", actor: "User", precondition: "Camera permission granted", flow: "User taps 'Choose Your Photo' → Selects 'Take a Photo' → Camera permission check → CameraX preview opens → User captures photo → Bitmap set as avatar", postcondition: "Avatar captured from camera" },
      { id: "UC-06.05", name: "Process Try-On (Backend/RapidAPI)", actor: "User", precondition: "Avatar + product image ready", flow: "User selects gender (male/female) → Taps 'Try On This Product' → TryOnViewModel.processTryOn() → Avatar base64/URL + product URL sent to backend /api/try-on-diffusion → Backend uploads avatar to Supabase → Calls RapidAPI → Returns result image URL → App downloads bitmap → Displays result", postcondition: "AI-generated try-on result displayed" },
      { id: "UC-06.06", name: "Process Try-On (Direct RapidAPI)", actor: "User", precondition: "Avatar + product image ready", flow: "TryOnRepository.processTryOnDirect() → Both images converted to multipart files → POST to try-on-diffusion.p.rapidapi.com/try-on-file → Response body decoded as bitmap → Displayed", postcondition: "Try-on result shown" },
      { id: "UC-06.07", name: "Process Try-On (HuggingFace IDM-VTON)", actor: "User", precondition: "Backend fallback mode", flow: "Backend /api/try-on → Predict method first (base64 images → Gradio /run/predict) → If fails, queue method (/queue/join → poll /queue/status) → Result base64 → Displayed", postcondition: "Try-on result via IDM-VTON" },
      { id: "UC-06.08", name: "Live AR Try-On (Snap Camera Kit)", actor: "User", precondition: "Product has lensId, camera permission granted", flow: "User triggers Live AR → LiveARTryOnActivity launched → Snap Camera Kit session created with CameraX source → Lens loaded by lensId from LENS_GROUP_ID → Real-time AR overlay on camera feed → User can flip camera", postcondition: "Live AR lens active on camera" },
      { id: "UC-06.09", name: "Retry Failed Try-On", actor: "User", precondition: "Try-on failed with retryable error", flow: "Error screen shows Retry button → User taps Retry → TryOnViewModel.retry() re-sends same request → May succeed on retry", postcondition: "Try-on retried" },
      { id: "UC-06.10", name: "Change Avatar and Retry", actor: "User", precondition: "Try-on completed or failed", flow: "User taps 'Choose Different Photo' or 'Try Again' → Avatar cleared → Selection screen shown → User picks new photo → Re-processes", postcondition: "New avatar selected for try-on" },
    ],
    testCases: [
      { id: "TC-06.01", title: "Navigate to Try-On from product", steps: "Open product detail → Tap Try On", expected: "TryOnScreen loads with product info card" },
      { id: "TC-06.02", title: "Select avatar from profile", steps: "Tap Choose Photo → Use Profile Photo", expected: "Profile image downloaded and shown as avatar preview" },
      { id: "TC-06.03", title: "Select avatar from gallery", steps: "Tap Choose Photo → Upload from Gallery → Pick image", expected: "Gallery image set as avatar preview" },
      { id: "TC-06.04", title: "Capture avatar from camera", steps: "Tap Choose Photo → Take a Photo → Grant permission → Capture", expected: "Camera photo set as avatar preview" },
      { id: "TC-06.05", title: "Process try-on successfully", steps: "Set avatar → Select gender → Tap Try On This Product", expected: "Loading state → Result image displayed" },
      { id: "TC-06.06", title: "Try-on with no internet", steps: "Disable internet → Attempt try-on", expected: "Error: 'No internet connection'" },
      { id: "TC-06.07", title: "Try-on timeout", steps: "Attempt try-on when API is slow", expected: "Error: 'Request timed out' with retry option" },
      { id: "TC-06.08", title: "Retry after failure", steps: "Get error → Tap Retry", expected: "Re-processes with same images" },
      { id: "TC-06.09", title: "Change avatar after result", steps: "View result → Tap Try Again", expected: "Avatar cleared, selection screen shown" },
      { id: "TC-06.10", title: "Gender selection toggle", steps: "Toggle between Male/Female chips", expected: "Selection updates, affects API request avatarSex parameter" },
      { id: "TC-06.11", title: "Live AR try-on launch", steps: "Open product with lensId → Trigger Live AR", expected: "Camera opens with AR lens overlay" },
      { id: "TC-06.12", title: "Camera flip in AR", steps: "In Live AR → Tap flip camera button", expected: "Camera switches between front and back" },
    ]
  },

  // ─── EPIC 7 ───
  {
    id: "EP-07",
    name: "Notifications",
    description: "In-app notification system for order updates, promotions, price drops, restocks, and delivery updates.",
    useCases: [
      { id: "UC-07.01", name: "View Notifications", actor: "User", precondition: "Notifications exist", flow: "User taps notification icon → NotificationScreen → NotificationViewModel loads from Room notifications table → Displays list with title, message, type icon, timestamp, read/unread state", postcondition: "Notifications displayed" },
      { id: "UC-07.02", name: "Mark as Read", actor: "User", precondition: "Unread notification exists", flow: "User taps notification → isRead set to true → NotificationDao updates → Unread count badge decreases", postcondition: "Notification marked as read" },
      { id: "UC-07.03", name: "Notification Badge", actor: "User", precondition: "Unread notifications exist", flow: "NotificationViewModel.unreadCount emitted as StateFlow → Badge shown on notification icon in toolbar", postcondition: "Badge reflects unread count" },
    ],
    testCases: [
      { id: "TC-07.01", title: "View notification list", steps: "Tap notification bell icon", expected: "List of notifications with type, title, time" },
      { id: "TC-07.02", title: "Mark notification as read", steps: "Tap an unread notification", expected: "Notification visual changes to read state" },
      { id: "TC-07.03", title: "Unread badge count", steps: "Receive 3 notifications, read 1", expected: "Badge shows 2" },
      { id: "TC-07.04", title: "Empty notifications", steps: "No notifications exist → View list", expected: "Empty state message shown" },
    ]
  },

  // ─── EPIC 8 ───
  {
    id: "EP-08",
    name: "Admin Panel (Product Management)",
    description: "Admin users can manage the product catalog: add new products, edit existing products, and delete products. All operations persist to Supabase with RLS enforcement.",
    useCases: [
      { id: "UC-08.01", name: "Admin Login", actor: "Admin", precondition: "User has admin role", flow: "Navigate to Admin Login screen → Enter credentials → AuthRepository.adminLogin() → Verify role → Navigate to AdminDashboard", postcondition: "Admin authenticated" },
      { id: "UC-08.02", name: "View Admin Dashboard", actor: "Admin", precondition: "Admin logged in", flow: "AdminDashboardScreen loads → Shows product list with edit/delete options → Add Product button available", postcondition: "Admin sees product management interface" },
      { id: "UC-08.03", name: "Add New Product", actor: "Admin", precondition: "Admin on Dashboard", flow: "Tap Add Product → AddProductScreen → Fill name, description, price, category, subCategory, images, sizes, colors, stock, brand, isFeatured, isNew → Submit → ProductRepository.addProductToSupabase() → Inserted into Supabase products table + Room cache", postcondition: "New product in catalog" },
      { id: "UC-08.04", name: "Edit Product", actor: "Admin", precondition: "Product exists", flow: "Tap Edit on product → AddProductScreen loads with productId → Pre-fills existing data → Admin modifies fields → Submit → ProductRepository.updateProductInSupabase() → Updated in Supabase + Room", postcondition: "Product updated" },
      { id: "UC-08.05", name: "Delete Product", actor: "Admin", precondition: "Product exists", flow: "Tap Delete on product → Confirmation dialog → ProductRepository.deleteProductFromSupabase() → Removed from Supabase + Room", postcondition: "Product removed from catalog" },
      { id: "UC-08.06", name: "Upload Product Images", actor: "Admin", precondition: "Adding/editing product", flow: "Admin selects images → StorageRepository uploads to Supabase Storage product-images bucket → Public URLs returned → Set in product images array", postcondition: "Images uploaded and linked to product" },
    ],
    testCases: [
      { id: "TC-08.01", title: "Add product with all fields", steps: "Admin Login → Add Product → Fill all fields → Submit", expected: "Product appears in catalog, synced to Supabase" },
      { id: "TC-08.02", title: "Edit product name", steps: "Admin → Edit product → Change name → Submit", expected: "Product name updated in Supabase and local DB" },
      { id: "TC-08.03", title: "Delete product", steps: "Admin → Delete product → Confirm", expected: "Product removed from Supabase and local DB" },
      { id: "TC-08.04", title: "Add product without required fields", steps: "Leave name/price empty → Submit", expected: "Validation error shown" },
      { id: "TC-08.05", title: "Non-admin access denied", steps: "Regular user tries to access admin routes", expected: "Access denied error" },
      { id: "TC-08.06", title: "Upload product image", steps: "Select image file → Upload to Supabase Storage", expected: "Image uploaded, URL set in product" },
      { id: "TC-08.07", title: "RLS enforcement", steps: "Non-admin user tries insert via Supabase", expected: "Policy error: 'Access denied. Admin privileges required.'" },
    ]
  },

  // ─── EPIC 9 ───
  {
    id: "EP-09",
    name: "App Settings & Theme",
    description: "User preferences, theme customization (light/dark mode), address management, help center, and about screen.",
    useCases: [
      { id: "UC-09.01", name: "Toggle Dark/Light Theme", actor: "User", precondition: "App running", flow: "User navigates to Settings → Toggle dark mode switch → ThemeViewModel updates → DataStore preference saved → App theme recomposes", postcondition: "Theme changed and persisted" },
      { id: "UC-09.02", name: "Manage Addresses", actor: "Authenticated User", precondition: "User logged in", flow: "Navigate to Settings → Manage Addresses → View/add/edit/delete shipping addresses → Set default address", postcondition: "Addresses managed" },
      { id: "UC-09.03", name: "View Help Center", actor: "User", precondition: "None", flow: "Navigate to Settings → Help Center → View FAQs and support options", postcondition: "Help information displayed" },
      { id: "UC-09.04", name: "View About Screen", actor: "User", precondition: "None", flow: "Navigate to Settings → About → View app version, credits, terms", postcondition: "About information displayed" },
    ],
    testCases: [
      { id: "TC-09.01", title: "Enable dark mode", steps: "Settings → Toggle dark mode ON", expected: "App switches to dark theme" },
      { id: "TC-09.02", title: "Dark mode persists", steps: "Enable dark mode → Kill app → Reopen", expected: "App opens in dark mode" },
      { id: "TC-09.03", title: "Add shipping address", steps: "Settings → Manage Addresses → Add new address", expected: "Address saved and shown in list" },
      { id: "TC-09.04", title: "View Help Center", steps: "Settings → Help Center", expected: "Help content displayed" },
      { id: "TC-09.05", title: "View About", steps: "Settings → About", expected: "App version and info shown" },
    ]
  },

  // ─── EPIC 10 ───
  {
    id: "EP-10",
    name: "Backend API & Infrastructure",
    description: "Node.js Express backend serving as API proxy for AI try-on services, product catalog endpoints, authentication stubs, and email confirmation handling. Deployed alongside Supabase cloud infrastructure.",
    useCases: [
      { id: "UC-10.01", name: "Try-On Diffusion API Proxy", actor: "Android App", precondition: "Backend running, RapidAPI key configured", flow: "POST /api/try-on-diffusion → Validate inputs → Upload avatar base64 to Supabase Storage → Call RapidAPI with clothing_image_url + avatar_image_url + avatar_sex → Return result image URL", postcondition: "Try-on result URL returned" },
      { id: "UC-10.02", name: "IDM-VTON API Proxy", actor: "Android App", precondition: "Backend running, HF Space available", flow: "POST /api/try-on → Validate inputs → Try Gradio predict endpoint → Fallback to queue method → Poll for result → Return base64 result", postcondition: "Try-on base64 result returned" },
      { id: "UC-10.03", name: "Health Check", actor: "Monitoring System", precondition: "Backend running", flow: "GET /health → Return status, timestamp, model info, endpoint URL", postcondition: "Health status returned" },
      { id: "UC-10.04", name: "Email Confirmation Redirect", actor: "User (via email)", precondition: "User registered, confirmation email sent", flow: "User clicks email link → Redirects to /auth/confirm → Serves static email-confirmed.html page", postcondition: "Email confirmed, user informed" },
      { id: "UC-10.05", name: "Product Listing API", actor: "Android App", precondition: "Backend running", flow: "GET /products → Returns hardcoded sample products array with pagination params → Used as fallback when Supabase is unavailable", postcondition: "Product list returned" },
    ],
    testCases: [
      { id: "TC-10.01", title: "Health check endpoint", steps: "GET /health", expected: "200 OK with status, model, endpoint info" },
      { id: "TC-10.02", title: "Try-on diffusion with valid data", steps: "POST /api/try-on-diffusion with valid userImage + productImageUrl", expected: "200 with success=true, resultImage URL" },
      { id: "TC-10.03", title: "Try-on with missing images", steps: "POST /api/try-on-diffusion without productImageUrl", expected: "400 with validation error message" },
      { id: "TC-10.04", title: "Try-on without API key", steps: "Remove RAPID_API_KEY → POST /api/try-on-diffusion", expected: "500 with 'RAPID_API_KEY missing' error" },
      { id: "TC-10.05", title: "IDM-VTON try-on", steps: "POST /api/try-on with valid base64 images", expected: "200 with success=true, resultImage base64" },
      { id: "TC-10.06", title: "Products listing", steps: "GET /products", expected: "200 with products array, total count, pagination" },
      { id: "TC-10.07", title: "404 for unknown endpoint", steps: "GET /unknown-path", expected: "404 with available endpoints list" },
      { id: "TC-10.08", title: "Email confirmation page", steps: "GET /auth/confirm", expected: "HTML confirmation page served" },
    ]
  }
];

// ── Build Document ───────────────────────────────────────────────────

const children = [];

// Title page
children.push(new Paragraph({ spacing: { before: 2000 } }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 }, children: [new TextRun({ text: "NextGenEcommerce", bold: true, size: 56, color: "1F4E79" })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 100 }, children: [new TextRun({ text: "Epics, Use Cases & Test Cases", bold: true, size: 36, color: "2E75B6" })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 100 }, children: [new TextRun({ text: "Comprehensive Software Requirements Documentation", size: 24, color: "666666", italics: true })] }));
children.push(new Paragraph({ spacing: { before: 400 } }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: `Document Version: 1.0  |  Date: ${new Date().toISOString().split("T")[0]}`, size: 20, color: "999999" })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 }, children: [new TextRun({ text: "Platform: Android (Kotlin/Jetpack Compose) + Node.js Backend + Supabase", size: 20, color: "999999" })] }));

// Table of Contents
children.push(new Paragraph({ children: [new PageBreak()] }));
children.push(heading("Table of Contents"));
epics.forEach((e, i) => {
  children.push(para(`${i + 1}. ${e.id} - ${e.name}`, { size: 22 }));
});

// Summary table
children.push(new Paragraph({ children: [new PageBreak()] }));
children.push(heading("Executive Summary"));
children.push(para("This document covers 10 epics, their associated use cases, and detailed test cases for the NextGenEcommerce Android application."));
children.push(new Paragraph({ spacing: { after: 150 } }));

const summaryRows = epics.map(e => [e.id, e.name, String(e.useCases.length), String(e.testCases.length)]);
summaryRows.push(["", "TOTAL", String(epics.reduce((s, e) => s + e.useCases.length, 0)), String(epics.reduce((s, e) => s + e.testCases.length, 0))]);
children.push(makeTable(["Epic ID", "Epic Name", "Use Cases", "Test Cases"], summaryRows, [10, 55, 17, 18]));

// Each epic
epics.forEach((epic, idx) => {
  children.push(new Paragraph({ children: [new PageBreak()] }));
  children.push(heading(`${idx + 1}. ${epic.id}: ${epic.name}`));
  children.push(para(epic.description, { italics: true, color: "555555" }));

  // Use Cases
  children.push(heading("Use Cases", HeadingLevel.HEADING_2));

  const ucRows = epic.useCases.map(uc => [
    uc.id,
    uc.name,
    uc.actor,
    uc.precondition,
    uc.flow,
    uc.postcondition
  ]);
  children.push(makeTable(
    ["ID", "Use Case", "Actor", "Precondition", "Main Flow", "Postcondition"],
    ucRows,
    [8, 12, 10, 12, 42, 16]
  ));

  children.push(new Paragraph({ spacing: { before: 300 } }));

  // Test Cases
  children.push(heading("Test Cases", HeadingLevel.HEADING_2));

  const tcRows = epic.testCases.map(tc => [
    tc.id,
    tc.title,
    tc.steps,
    tc.expected
  ]);
  children.push(makeTable(
    ["ID", "Test Case Title", "Steps", "Expected Result"],
    tcRows,
    [10, 22, 34, 34]
  ));
});

// Appendix
children.push(new Paragraph({ children: [new PageBreak()] }));
children.push(heading("Appendix: Technology Stack Reference"));
const techRows = [
  ["Android App", "Kotlin, Jetpack Compose (Material 3), Room, Hilt, Retrofit, Coil, CameraX, ARCore, Snap Camera Kit, ML Kit, TensorFlow Lite"],
  ["Backend", "Node.js, Express.js, Axios, Supabase JS Client"],
  ["Cloud Services", "Supabase (Auth, Postgrest DB, Storage), Hugging Face Spaces (IDM-VTON), RapidAPI (Try-On Diffusion)"],
  ["Build System", "Gradle 8.13, KSP, Android Gradle Plugin 8.13.2"],
  ["Architecture", "MVVM, Repository Pattern, Dependency Injection (Hilt), Offline-First (Room cache)"],
  ["AI/ML", "Try-On Diffusion API (RapidAPI), IDM-VTON (Hugging Face), Snap Camera Kit Lenses, ML Kit Pose Detection"],
  ["Navigation", "Jetpack Navigation Compose with 25+ screens, Bottom Navigation (5 tabs)"],
];
children.push(makeTable(["Component", "Technologies"], techRows, [20, 80]));

const doc = new Document({
  creator: "NextGenEcommerce Team",
  title: "NextGenEcommerce - Epics, Use Cases & Test Cases",
  description: "Comprehensive software requirements documentation",
  sections: [{ children }]
});

Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync("NextGenEcommerce_Epics_UseCases_TestCases.docx", buffer);
  console.log("Document generated successfully!");
  console.log("Path: C:\\NextGenEcommerce\\Diagrams\\NextGenEcommerce_Epics_UseCases_TestCases.docx");
}).catch(err => {
  console.error("Error:", err);
  process.exit(1);
});
