// server.js - Complete IDM-VTON Virtual Try-On Backend
// Uses Hugging Face Spaces Gradio API (yisol/IDM-VTON)
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const axios = require('axios');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json({ limit: '50mb' }));
app.use(bodyParser.urlencoded({ limit: '50mb', extended: true }));

// Hugging Face Spaces configuration
const HF_SPACE_URL = 'https://yisol-idm-vton.hf.space';

/**
 * Extract clean base64 data from data URL
 */
function extractBase64(base64Data) {
    if (!base64Data) return '';
    
    // Remove data:image/...;base64, prefix if present
    const base64Pattern = /^data:image\/\w+;base64,/;
    const cleanData = base64Data.replace(base64Pattern, '').replace(/\s/g, '');
    
    return cleanData;
}

/**
 * Delay utility
 */
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Call Hugging Face Gradio Space API using queue method
 */
async function callGradioQueue(userImageBase64, productImageBase64) {
    try {
        console.log('🔗 Connecting to Hugging Face Space (Queue Method)...');
        
        const userImg = extractBase64(userImageBase64);
        const productImg = extractBase64(productImageBase64);
        
        // Generate session hash
        const sessionHash = Math.random().toString(36).substring(2, 15);
        
        // Step 1: Join queue
        const joinResponse = await axios.post(`${HF_SPACE_URL}/queue/join`, {
            data: [
                {
                    background: `data:image/jpeg;base64,${userImg}`,
                    layers: [],
                    composite: null
                },
                `data:image/jpeg;base64,${productImg}`,
                "", // garment_des
                true, // is_checked (use auto-mask)
                false, // is_checked_crop
                30, // denoise_steps
                42 // seed
            ],
            fn_index: 0,
            session_hash: sessionHash
        }, {
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 10000
        });

        console.log('✓ Joined queue with session:', sessionHash);

        // Step 2: Poll for result
        let attempts = 0;
        const maxAttempts = 90; // 90 attempts * 2s = 3 minutes max

        while (attempts < maxAttempts) {
            await delay(2000); // Wait 2 seconds between polls
            attempts++;
            
            try {
                const statusResponse = await axios.get(`${HF_SPACE_URL}/queue/status`, {
                    params: { session_hash: sessionHash },
                    timeout: 5000
                });

                const status = statusResponse.data;
                
                if (status.status === 'COMPLETE' && status.data && status.data.data) {
                    console.log('✅ Processing complete!');
                    
                    const resultData = status.data.data[0];
                    
                    // If it's a URL, fetch the image
                    if (typeof resultData === 'string' && resultData.startsWith('http')) {
                        console.log('📥 Downloading result image from URL...');
                        const imageResponse = await axios.get(resultData, {
                            responseType: 'arraybuffer',
                            timeout: 30000
                        });
                        return Buffer.from(imageResponse.data).toString('base64');
                    }
                    
                    // If it's base64 data
                    if (typeof resultData === 'string') {
                        return extractBase64(resultData);
                    }
                    
                    throw new Error('Invalid result format from API');
                }

                if (status.status === 'FAILED') {
                    throw new Error('Processing failed: ' + (status.error || 'Unknown error'));
                }
                
                // Log progress
                if (attempts % 10 === 0) {
                    console.log(`⏳ Still processing... (${attempts}/${maxAttempts})`);
                }

            } catch (pollError) {
                if (pollError.code === 'ECONNABORTED') {
                    console.log('⚠️ Poll timeout, retrying...');
                    continue;
                }
                throw pollError;
            }
        }

        throw new Error('Processing timeout - no result after 3 minutes');

    } catch (error) {
        console.error('❌ Queue API Error:', error.message);
        throw error;
    }
}

/**
 * Call Hugging Face Gradio Space API using direct predict method
 */
async function callGradioPredict(userImageBase64, productImageBase64) {
    try {
        console.log('🚀 Using Gradio predict endpoint...');
        
        const userImg = extractBase64(userImageBase64);
        const productImg = extractBase64(productImageBase64);
        
        const payload = {
            data: [
                {
                    background: `data:image/jpeg;base64,${userImg}`,
                    layers: [],
                    composite: null
                },
                `data:image/jpeg;base64,${productImg}`,
                "", // garment description
                true, // use auto-mask
                false, // don't crop
                30, // denoise steps
                42 // seed
            ]
        };

        const response = await axios.post(`${HF_SPACE_URL}/run/predict`, payload, {
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 120000 // 2 minute timeout
        });

        if (response.data && response.data.data && response.data.data[0]) {
            const resultData = response.data.data[0];
            
            // Handle URL response
            if (typeof resultData === 'string' && resultData.startsWith('http')) {
                console.log('📥 Downloading result from URL...');
                const imageResponse = await axios.get(resultData, {
                    responseType: 'arraybuffer',
                    timeout: 30000
                });
                return Buffer.from(imageResponse.data).toString('base64');
            }
            
            // Handle base64 response
            if (typeof resultData === 'string') {
                return extractBase64(resultData);
            }
            
            throw new Error('Invalid response format');
        }

        throw new Error('No data in response');

    } catch (error) {
        console.error('❌ Predict API Error:', error.message);
        throw error;
    }
}

/**
 * Virtual Try-On API endpoint
 */
app.post('/api/try-on', async (req, res) => {
    const startTime = Date.now();
    
    try {
        const { userImage, productImage, productName, productColor, productType } = req.body;
        
        // Validation
        if (!userImage || !productImage) {
            return res.status(400).json({
                success: false,
                message: 'Both user image and product image are required'
            });
        }

        console.log('========================================');
        console.log('🎯 Processing virtual try-on request...');
        console.log('📦 Product:', productName || 'Unknown', productColor || '', productType || '');
        console.log('⏰ Started at:', new Date().toLocaleTimeString());
        console.log('========================================');

        let resultImageBase64;
        let method = 'unknown';
        
        // Try predict method first (faster if it works)
        try {
            console.log('🔄 Attempting direct predict method...');
            resultImageBase64 = await callGradioPredict(userImage, productImage);
            method = 'predict';
        } catch (predictError) {
            console.log('⚠️ Predict method failed, trying queue method...');
            console.log('Error:', predictError.message);
            
            // Fallback to queue method
            try {
                resultImageBase64 = await callGradioQueue(userImage, productImage);
                method = 'queue';
            } catch (queueError) {
                console.error('❌ Queue method also failed:', queueError.message);
                throw queueError;
            }
        }

        const processingTime = ((Date.now() - startTime) / 1000).toFixed(2);
        
        console.log('========================================');
        console.log('✅ Virtual try-on completed successfully!');
        console.log('⏱️  Processing time:', processingTime, 'seconds');
        console.log('📊 Result image size:', (resultImageBase64.length / 1024).toFixed(2), 'KB');
        console.log('🔧 Method used:', method);
        console.log('========================================');

        res.json({
            success: true,
            resultImage: resultImageBase64,
            description: `Successfully applied ${productColor || ''} ${productName || 'product'} to your photo using AI`,
            message: 'Virtual try-on completed with IDM-VTON',
            processingTime: processingTime,
            method: method
        });

    } catch (error) {
        const processingTime = ((Date.now() - startTime) / 1000).toFixed(2);
        
        console.error('========================================');
        console.error('❌ Error in try-on API:');
        console.error('Error message:', error.message);
        console.error('Processing time before error:', processingTime, 'seconds');
        console.error('========================================');
        
        let errorMessage = 'Failed to process virtual try-on. Please try again.';
        let statusCode = 500;
        
        if (error.message.includes('timeout')) {
            errorMessage = 'Request timeout. The AI model is taking too long. Please try again in a moment.';
            statusCode = 504;
        } else if (error.message.includes('ECONNREFUSED') || error.message.includes('ENOTFOUND')) {
            errorMessage = 'Cannot connect to Hugging Face Space. The service may be temporarily unavailable.';
            statusCode = 503;
        } else if (error.code === 'ECONNABORTED') {
            errorMessage = 'Connection timeout. Please try again.';
            statusCode = 504;
        }
        
        res.status(statusCode).json({
            success: false,
            message: errorMessage,
            error: error.message,
            processingTime: processingTime
        });
    }
});

/**
 * Health check endpoint
 */
app.get('/health', (req, res) => {
    res.json({
        status: 'OK',
        timestamp: new Date().toISOString(),
        message: 'Server running with Hugging Face IDM-VTON Spaces API',
        model: 'yisol/IDM-VTON',
        endpoint: HF_SPACE_URL,
        port: PORT
    });
});

/**
 * Root endpoint
 */
app.get('/', (req, res) => {
    res.json({
        service: 'Next Gen Ecommerce - Virtual Try-On API',
        version: '1.0.0',
        status: 'online',
        endpoints: {
            health: '/health',
            tryOn: '/api/try-on (POST)'
        },
        documentation: 'Send POST request to /api/try-on with userImage and productImage as base64'
    });
});

/**
 * 404 handler
 */
app.use((req, res) => {
    res.status(404).json({
        success: false,
        message: 'Endpoint not found',
        availableEndpoints: ['GET /', 'GET /health', 'POST /api/try-on']
    });
});

/**
 * Error handler
 */
app.use((err, req, res, next) => {
    console.error('Unhandled error:', err);
    res.status(500).json({
        success: false,
        message: 'Internal server error',
        error: err.message
    });
});

/**
 * Products API Endpoints
 */
app.get('/products', (req, res) => {
    const { category, page = 1, limit = 20 } = req.query;

    // Sample products data
    const sampleProducts = [
        {
            id: '1',
            name: 'Classic Black T-Shirt',
            description: 'Premium cotton t-shirt with modern fit',
            price: 29.99,
            originalPrice: 39.99,
            category: 'CLOTHING',
            subCategory: 'T-Shirts',
            images: ['https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400'],
            sizes: ['XS', 'S', 'M', 'L', 'XL'],
            colors: ['Black', 'White', 'Gray'],
            rating: 4.5,
            reviewCount: 156,
            stock: 50,
            isFeatured: true,
            brand: 'NextGen Fashion'
        },
        {
            id: '2',
            name: 'Summer Floral Dress',
            description: 'Light and breezy summer dress',
            price: 59.99,
            originalPrice: 79.99,
            category: 'CLOTHING',
            subCategory: 'Dresses',
            images: ['https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=400'],
            sizes: ['XS', 'S', 'M', 'L'],
            colors: ['Floral Blue', 'Floral Pink'],
            rating: 4.8,
            reviewCount: 203,
            stock: 30,
            isFeatured: true,
            isNew: true,
            brand: 'NextGen Fashion'
        }
    ];

    res.json({
        success: true,
        products: sampleProducts,
        total: sampleProducts.length,
        page: parseInt(page),
        totalPages: 1
    });
});

app.get('/products/:id', (req, res) => {
    const { id } = req.params;

    const product = {
        id: id,
        name: 'Sample Product',
        description: 'This is a sample product',
        price: 29.99,
        originalPrice: 39.99,
        category: 'CLOTHING',
        images: ['https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400'],
        sizes: ['S', 'M', 'L', 'XL'],
        colors: ['Black', 'White'],
        rating: 4.5,
        reviewCount: 100,
        stock: 50,
        brand: 'NextGen'
    };

    res.json({ success: true, product });
});

app.get('/products/featured', (req, res) => {
    const featuredProducts = [
        // Add featured products here
    ];

    res.json({
        success: true,
        products: featuredProducts,
        total: featuredProducts.length
    });
});

/**
 * Orders API Endpoints
 */
app.post('/orders', (req, res) => {
    const order = req.body;

    const newOrder = {
        id: 'ORD' + Date.now(),
        ...order,
        status: 'PENDING',
        paymentStatus: 'COMPLETED',
        createdAt: Date.now(),
        updatedAt: Date.now()
    };

    res.json({
        success: true,
        order: newOrder
    });
});

app.get('/orders/:id', (req, res) => {
    const { id } = req.params;

    res.json({
        success: true,
        order: {
            id: id,
            status: 'PROCESSING',
            items: [],
            total: 99.99
        }
    });
});

app.get('/orders/user/:userId', (req, res) => {
    const { userId } = req.params;

    res.json({
        success: true,
        orders: []
    });
});

/**
 * Auth API Endpoints
 */
app.post('/auth/register', (req, res) => {
    const { email, password, name } = req.body;

    res.json({
        success: true,
        token: 'sample_token_' + Date.now(),
        user: {
            id: 'USER' + Date.now(),
            email,
            name,
            createdAt: Date.now()
        }
    });
});

app.post('/auth/login', (req, res) => {
    const { email, password } = req.body;

    res.json({
        success: true,
        token: 'sample_token_' + Date.now(),
        user: {
            id: 'USER123',
            email,
            name: 'Sample User',
            createdAt: Date.now()
        }
    });
});

/**
 * Reviews API Endpoints
 */
app.post('/reviews', (req, res) => {
    const review = req.body;

    res.json({
        success: true,
        review: {
            id: 'REV' + Date.now(),
            ...review,
            createdAt: Date.now()
        }
    });
});

app.get('/reviews/product/:productId', (req, res) => {
    const { productId } = req.params;

    res.json({
        success: true,
        reviews: [],
        averageRating: 4.5,
        totalReviews: 0
    });
});

/**
 * Start server
 */
app.listen(PORT, () => {
    console.log('\n' + '='.repeat(60));
    console.log('✅ Next Gen Ecommerce Backend - Full API');
    console.log('='.repeat(60));
    console.log(`🚀 Model: Hugging Face IDM-VTON Spaces`);
    console.log(`🌐 Server: http://localhost:${PORT}`);
    console.log(`📡 Try-On API: POST /api/try-on`);
    console.log(`🛍️  Products API: GET /products`);
    console.log(`📦 Orders API: POST /orders`);
    console.log(`👤 Auth API: POST /auth/login, /auth/register`);
    console.log(`💚 Health Check: GET /health`);
    console.log('='.repeat(60));
    console.log('📝 Ready to receive requests!');
    console.log('⚠️  First try-on request may take 30-60 seconds');
    console.log('='.repeat(60) + '\n');
});