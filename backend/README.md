# NextGen E-Commerce Backend

Express.js backend API for NextGen E-Commerce app with virtual try-on capabilities using Hugging Face IDM-VTON.

## 🚀 Features

- **Virtual Try-On API** - AI-powered clothing try-on using IDM-VTON
- **Products API** - Product catalog management
- **Orders API** - Order creation and tracking
- **Authentication API** - User registration and login
- **Reviews API** - Product reviews and ratings

## 📋 Prerequisites

- Node.js 16.x or later
- npm 8.x or later

## 🔧 Installation

```bash
# Install dependencies
npm install

# Start development server
npm start

# Start with auto-reload (if nodemon is configured)
npm run dev
```

## 📡 API Endpoints

### Health Check
```http
GET /health
```

Returns server status and configuration.

### Virtual Try-On
```http
POST /api/try-on
Content-Type: application/json

{
  "userImage": "base64_encoded_image",
  "productImage": "base64_encoded_image",
  "productName": "T-Shirt",
  "productColor": "Black",
  "productType": "Shirt"
}
```

**Response:**
```json
{
  "success": true,
  "resultImage": "base64_encoded_result",
  "message": "Virtual try-on completed",
  "processingTime": "45.23"
}
```

### Products

#### Get All Products
```http
GET /products?category=CLOTHING&page=1&limit=20
```

#### Get Product by ID
```http
GET /products/:id
```

#### Get Featured Products
```http
GET /products/featured
```

### Orders

#### Create Order
```http
POST /orders
Content-Type: application/json

{
  "userId": "USER123",
  "items": [...],
  "shippingAddress": {...},
  "paymentMethod": "credit_card"
}
```

#### Get Order by ID
```http
GET /orders/:id
```

#### Get User Orders
```http
GET /orders/user/:userId
```

### Authentication

#### Register
```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe"
}
```

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### Reviews

#### Submit Review
```http
POST /reviews
Content-Type: application/json

{
  "productId": "PROD123",
  "userId": "USER123",
  "rating": 5,
  "comment": "Great product!"
}
```

#### Get Product Reviews
```http
GET /reviews/product/:productId
```

## 🔑 Environment Variables

Create a `.env` file:

```env
PORT=3000
NODE_ENV=development
```

## 🤖 Virtual Try-On Details

The virtual try-on feature uses **Hugging Face IDM-VTON** model:

- **Model**: yisol/IDM-VTON
- **Processing Time**: 30-60 seconds (first request), 15-30 seconds (subsequent)
- **Input**: User image + Product image (base64)
- **Output**: Composite image with product worn by user

### Try-On Flow
1. Client sends user image and product image (base64 encoded)
2. Server converts to proper format
3. Sends to Hugging Face Spaces Gradio API
4. Polls for result (queue system)
5. Downloads result image
6. Returns to client as base64

## 📊 Response Format

All API responses follow this structure:

```json
{
  "success": true|false,
  "data": {...},
  "message": "Optional message",
  "error": "Error details if success=false"
}
```

## 🐛 Error Handling

Common error responses:

- **400 Bad Request** - Missing or invalid parameters
- **404 Not Found** - Resource not found
- **500 Internal Server Error** - Server error
- **503 Service Unavailable** - Hugging Face API unavailable
- **504 Gateway Timeout** - Request timeout

## 🔄 Try-On Error Handling

The try-on endpoint implements fallback mechanisms:
1. Attempts direct predict method
2. Falls back to queue method if predict fails
3. Returns appropriate error if both fail

## 📝 Logging

Server logs include:
- Request details
- Processing times
- Error messages
- API call results

Example log output:
```
========================================
🎯 Processing virtual try-on request...
📦 Product: T-Shirt Black Shirt
⏰ Started at: 10:30:45 AM
========================================
🔄 Attempting direct predict method...
✅ Virtual try-on completed successfully!
⏱️  Processing time: 42.15 seconds
📊 Result image size: 256.78 KB
========================================
```

## 🚀 Deployment

### Local Development
```bash
npm start
```

### Production Deployment

#### Heroku
```bash
heroku create nextgen-ecommerce-api
git push heroku main
```

#### Railway
```bash
railway init
railway up
```

#### Docker
```dockerfile
FROM node:16
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
EXPOSE 3000
CMD ["node", "server.js"]
```

## 🔒 Security Notes

**Current Setup** (Development only):
- No authentication required
- CORS enabled for all origins
- Cleartext HTTP allowed

**Production Recommendations**:
- Add JWT authentication
- Restrict CORS to specific domains
- Use HTTPS only
- Add rate limiting
- Implement API keys
- Add request validation

## 📈 Performance

- **Max payload**: 50MB (for base64 images)
- **Timeout**: 60 seconds per request
- **Concurrent requests**: Limited by Hugging Face API

## 🧪 Testing

Test the API with curl:

```bash
# Health check
curl http://localhost:3000/health

# Get products
curl http://localhost:3000/products

# Create order
curl -X POST http://localhost:3000/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"USER123","items":[],"total":99.99}'
```

## 📚 Dependencies

- **express**: Web framework
- **cors**: CORS middleware
- **body-parser**: Request body parsing
- **axios**: HTTP client for Hugging Face API
- **dotenv**: Environment variables

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## 📄 License

MIT License

---

Built with ❤️ using Node.js and Express
