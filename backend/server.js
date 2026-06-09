// server.js - Complete IDM-VTON Virtual Try-On Backend
// Uses Hugging Face Spaces Gradio API (yisol/IDM-VTON)
const express = require('express');
const cors = require('cors');
const axios = require('axios');
const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const path = require('path');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json({ limit: '20mb' }));
app.use(express.urlencoded({ limit: '20mb', extended: true }));

// Serve static files from public directory
app.use(express.static(path.join(__dirname, 'public')));

// Email confirmation redirect page
app.get('/auth/confirm', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'email-confirmed.html'));
});

app.get('/account/delete', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'account-deletion.html'));
});

app.get('/privacy-policy', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'privacy-policy.html'));
});

app.get('/terms', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'terms.html'));
});

// Hugging Face Spaces configuration
const HF_SPACE_URL = 'https://yisol-idm-vton.hf.space';

// Supabase Configuration
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_KEY;
const supabase = (supabaseUrl && supabaseKey) ? createClient(supabaseUrl, supabaseKey) : null;

// RapidAPI Configuration
const rapidApiKey = process.env.RAPID_API_KEY;
const rapidApiHost = 'try-on-diffusion.p.rapidapi.com';
const ADMIN_ROLE = 'admin';
const VALID_ORDER_STATUSES = new Set([
    'PENDING',
    'CONFIRMED',
    'PROCESSING',
    'PACKED',
    'READY_FOR_PICKUP',
    'SHIPPED',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'CANCELLED',
    'RETURN_REQUESTED',
    'RETURN_APPROVED',
    'RETURN_IN_TRANSIT',
    'RETURN_RECEIVED',
    'RETURNED',
    'RETURN_REJECTED'
]);

function getBearerToken(req) {
    const header = req.headers.authorization || '';
    const [scheme, token] = header.split(' ');
    return scheme?.toLowerCase() === 'bearer' ? token : null;
}

async function requireAuth(req, res, next) {
    if (!supabase) {
        return res.status(503).json({ success: false, message: 'Database is not configured' });
    }

    const token = getBearerToken(req);
    if (!token) {
        return res.status(401).json({ success: false, message: 'Missing bearer token' });
    }

    try {
        const { data, error } = await supabase.auth.getUser(token);
        if (error || !data?.user) {
            return res.status(401).json({ success: false, message: 'Invalid bearer token' });
        }
        req.auth = { user: data.user, token };
        next();
    } catch (error) {
        return res.status(401).json({ success: false, message: 'Invalid bearer token' });
    }
}

async function getUserRole(userId) {
    const { data, error } = await supabase
        .from('users')
        .select('role')
        .eq('id', userId)
        .single();

    if (error) return null;
    return data?.role || null;
}

async function isAdminUser(userId) {
    return (await getUserRole(userId)) === ADMIN_ROLE;
}

async function requireAdmin(req, res, next) {
    if (!(await isAdminUser(req.auth.user.id))) {
        return res.status(403).json({ success: false, message: 'Admin access required' });
    }
    next();
}

async function requireSameUserOrAdmin(req, res, next) {
    if (req.params.userId === req.auth.user.id || await isAdminUser(req.auth.user.id)) {
        return next();
    }
    return res.status(403).json({ success: false, message: 'Access denied' });
}

async function loadPricedOrderItems(items) {
    if (!Array.isArray(items) || items.length === 0) {
        throw new Error('Order must contain at least one item');
    }

    const requestedItems = items.map(item => ({
        ...item,
        productId: item.productId || item.product_id,
        quantity: Math.max(1, Math.min(99, Number.parseInt(item.quantity, 10) || 1))
    }));

    const productIds = [...new Set(requestedItems.map(item => item.productId).filter(Boolean))];
    if (productIds.length !== requestedItems.length) {
        throw new Error('Every order item must include a productId');
    }

    const { data: products, error } = await supabase
        .from('products')
        .select('id,name,price,image_url,stock,retailer_id')
        .in('id', productIds);

    if (error) throw error;

    const productsById = new Map((products || []).map(product => [String(product.id), product]));
    return requestedItems.map(item => {
        const product = productsById.get(String(item.productId));
        if (!product) {
            throw new Error(`Product not found: ${item.productId}`);
        }

        const stock = Number(product.stock ?? 0);
        if (stock > 0 && item.quantity > stock) {
            throw new Error(`Insufficient stock for ${product.name}`);
        }

        return {
            ...item,
            productId: String(product.id),
            productName: product.name,
            productImage: product.image_url || item.productImage || '',
            price: Number(product.price),
            retailerId: product.retailer_id || item.retailerId || null
        };
    });
}

async function assertAddressBelongsToUser(addressId, userId) {
    if (!addressId) return;

    const { data, error } = await supabase
        .from('shipping_addresses')
        .select('id,user_id')
        .eq('id', addressId)
        .single();

    if (error || !data) {
        throw new Error('Shipping address not found');
    }

    if (data.user_id !== userId) {
        const err = new Error('Shipping address does not belong to the current user');
        err.statusCode = 403;
        throw err;
    }
}

function getSafepayConfig() {
    const environment = process.env.SAFEPAY_ENV || 'sandbox';
    const apiKey = process.env.SAFEPAY_PUBLIC_KEY;
    const v1Secret = process.env.SAFEPAY_SECRET_KEY;
    const webhookSecret = process.env.SAFEPAY_WEBHOOK_SECRET;

    if (!apiKey || !v1Secret || !webhookSecret) {
        throw new Error('Safepay server credentials are not fully configured');
    }

    return {
        environment,
        apiKey,
        v1Secret,
        webhookSecret
    };
}

function safepayApiBase(environment) {
    if (environment === 'production') return 'https://api.getsafepay.com';
    if (environment === 'development') return 'https://dev.api.getsafepay.com';
    return 'https://sandbox.api.getsafepay.com';
}

function safepayCheckoutBase(environment) {
    if (environment === 'production') return 'https://getsafepay.com/checkout/pay';
    if (environment === 'development') return 'https://dev.api.getsafepay.com/checkout/pay';
    return 'https://sandbox.api.getsafepay.com/checkout/pay';
}

async function createSafepayPayment(amount, currency) {
    const config = getSafepayConfig();
    const response = await axios.post(`${safepayApiBase(config.environment)}/order/v1/init`, {
        amount,
        client: config.apiKey,
        currency,
        environment: config.environment
    });

    return response.data?.data;
}

function createSafepayCheckoutUrl({ token, orderId }) {
    const config = getSafepayConfig();
    const params = new URLSearchParams({
        beacon: token,
        cancel_url: process.env.SAFEPAY_CANCEL_URL || 'nextgenecommerce://payment/cancel',
        env: config.environment,
        order_id: orderId,
        redirect_url: process.env.SAFEPAY_REDIRECT_URL || 'nextgenecommerce://payment/success',
        source: 'custom',
        webhooks: 'true'
    });

    return `${safepayCheckoutBase(config.environment)}?${params.toString()}`;
}

function verifySafepayWebhook(req) {
    const { webhookSecret } = getSafepayConfig();
    const signature = req.headers['x-sfpy-signature'];
    if (!signature || !req.body?.data) return false;

    const expected = crypto
        .createHmac('sha512', webhookSecret)
        .update(Buffer.from(JSON.stringify(req.body.data)))
        .digest('hex');

    const signatureBuffer = Buffer.from(String(signature), 'hex');
    const expectedBuffer = Buffer.from(expected, 'hex');

    return signatureBuffer.length === expectedBuffer.length
        && crypto.timingSafeEqual(signatureBuffer, expectedBuffer);
}

function extractSafepayOrderId(event) {
    const data = event?.data || {};
    return data.order_id
        || data.orderId
        || data.reference
        || data.metadata?.order_id
        || data.tracker?.order_id
        || data.tracker?.metadata?.order_id
        || null;
}

function extractSafepayTracker(event) {
    const data = event?.data || {};
    const tracker = data.tracker || data.payment?.tracker;
    if (typeof tracker === 'string') return tracker;
    return tracker?.token || data.token || data.beacon || null;
}

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
 * Upload Base64 image to Supabase Storage and return public URL
 */
async function uploadToSupabase(base64Data, folder = 'temp-avatars') {
    if (!supabase) throw new Error('Supabase client not initialized');

    try {
        const buffer = Buffer.from(extractBase64(base64Data), 'base64');
        const fileName = `${folder}/${Date.now()}_${Math.random().toString(36).substring(7)}.jpg`;

        const { data, error } = await supabase
            .storage
            .from('product-images') // Using product-images bucket as requested
            .upload(fileName, buffer, {
                contentType: 'image/jpeg',
                upsert: true
            });

        if (error) throw error;

        // Get Public URL
        const { data: publicUrlData } = supabase
            .storage
            .from('product-images')
            .getPublicUrl(fileName);

        return publicUrlData.publicUrl;
    } catch (error) {
        console.error('Supabase Upload Error:', error.message);
        throw new Error('Failed to upload image to storage');
    }
}

/**
 * Try-On Diffusion API (RapidAPI) Endpoint
 */
app.post('/api/try-on-diffusion', async (req, res) => {
    const startTime = Date.now();

    try {
        const { userImage, avatarImageUrl, productImageUrl, avatarSex = 'female', clothingPrompt } = req.body;

        // Validation: need either userImage (base64) or avatarImageUrl, plus productImageUrl
        if ((!userImage && !avatarImageUrl) || !productImageUrl) {
            return res.status(400).json({
                success: false,
                message: 'Product image URL and either user image (base64) or avatar image URL are required'
            });
        }

        if (!rapidApiKey) {
            return res.status(500).json({
                success: false,
                message: 'Server configuration error: RAPID_API_KEY missing'
            });
        }

        console.log('========================================');
        console.log('Processing Try-On Diffusion request...');
        console.log('Product URL:', productImageUrl);
        console.log('Avatar Sex:', avatarSex);
        console.log('Avatar source:', avatarImageUrl ? 'URL (profile)' : 'Base64 (upload)');
        console.log('========================================');

        // 1. Get avatar URL - either use provided URL directly or upload base64
        let avatarUrl;
        if (avatarImageUrl) {
            // User's profile image URL provided directly - no upload needed
            avatarUrl = avatarImageUrl;
            console.log('Using provided avatar URL:', avatarUrl);
        } else {
            // Upload base64 avatar to Supabase to get public URL
            console.log('Uploading avatar to Supabase...');
            avatarUrl = await uploadToSupabase(userImage, 'temp-avatars');
            console.log('Avatar uploaded, URL:', avatarUrl);
        }

        // 2. Call RapidAPI
        console.log('🚀 Calling Try-On Diffusion API...');
        const options = {
            method: 'POST',
            url: 'https://try-on-diffusion.p.rapidapi.com/try-on-url',
            headers: {
                'content-type': 'application/json',
                'X-RapidAPI-Key': rapidApiKey,
                'X-RapidAPI-Host': rapidApiHost
            },
            data: {
                clothing_image_url: productImageUrl,
                avatar_image_url: avatarUrl,
                avatar_sex: avatarSex
            },
            timeout: 60000 // 60s timeout
        };

        const response = await axios.request(options);
        
        // The API returns the image URL directly or in a specific format?
        // Checking RapidAPI docs for "Try-On Diffusion":
        // Usually returns { url: "..." } or the image itself.
        // Assuming JSON response based on "Content-Type: application/json" request.
        // Let's assume response.data is the result or contains the url.
        // Common pattern: response.data[0] or response.data.url
        
        console.log('✅ API Response received');
        
        // Handle varied response formats safely
        let resultImageUrl = null;
        if (typeof response.data === 'string' && response.data.startsWith('http')) {
             resultImageUrl = response.data;
        } else if (response.data && response.data.url) {
             resultImageUrl = response.data.url;
        } else if (Array.isArray(response.data) && response.data[0]) {
             resultImageUrl = response.data[0];
        } else {
             // Fallback: Just return the data if we can't parse it, frontend might handle it
             resultImageUrl = response.data; 
        }

        const processingTime = ((Date.now() - startTime) / 1000).toFixed(2);

        res.json({
            success: true,
            resultImage: resultImageUrl, // Returns URL
            message: 'Virtual try-on completed successfully',
            processingTime: processingTime
        });

    } catch (error) {
        const processingTime = ((Date.now() - startTime) / 1000).toFixed(2);
        console.error('❌ Error in Try-On Diffusion API:', error.message);
        if (error.response) {
            console.error('API Response Data:', error.response.data);
        }

        res.status(500).json({
            success: false,
            message: 'Failed to process virtual try-on',
            error: error.message,
            details: error.response ? error.response.data : null,
            processingTime: processingTime
        });
    }
});

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
            description: 'Premium cotton t-shirt with modern fit. Perfect for casual wear.',
            price: 29.99,
            originalPrice: 39.99,
            category: 'CLOTHING',
            subCategory: 'T-Shirts',
            images: ['https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400'],
            localImageName: 'product_1.jpg',
            sizes: ['XS', 'S', 'M', 'L', 'XL', 'XXL'],
            colors: ['Black', 'White', 'Gray', 'Navy'],
            rating: 4.5,
            reviewCount: 156,
            stock: 50,
            isFeatured: true,
            brand: 'NextGen Fashion'
        },
        {
            id: '2',
            name: 'Summer Floral Dress',
            description: 'Light and breezy summer dress with beautiful floral print.',
            price: 59.99,
            originalPrice: 79.99,
            category: 'CLOTHING',
            subCategory: 'Dresses',
            images: ['https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=400'],
            localImageName: 'product_2.jpg',
            sizes: ['XS', 'S', 'M', 'L', 'XL'],
            colors: ['Floral Blue', 'Floral Pink', 'Floral Yellow'],
            rating: 4.8,
            reviewCount: 203,
            stock: 30,
            isFeatured: true,
            isNew: true,
            brand: 'NextGen Fashion'
        },
        {
            id: '3',
            name: 'Denim Jacket',
            description: 'Classic denim jacket with modern fit.',
            price: 79.99,
            originalPrice: 99.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1551028719-00167b16eac5?w=400'],
            localImageName: 'product_3.jpg',
            sizes: ['S', 'M', 'L', 'XL', 'XXL'],
            colors: ['Light Blue', 'Dark Blue', 'Black'],
            rating: 4.6,
            reviewCount: 98,
            stock: 25,
            isFeatured: false,
            brand: 'NextGen Fashion'
        },
        {
            id: '4',
            name: 'Formal White Shirt',
            description: 'Crisp white formal shirt perfect for professional settings.',
            price: 45.99,
            originalPrice: 59.99,
            category: 'CLOTHING',
            subCategory: 'Shirts',
            images: ['https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=400'],
            localImageName: 'product_4.jpg',
            sizes: ['S', 'M', 'L', 'XL', 'XXL'],
            colors: ['White', 'Light Blue', 'Pink'],
            rating: 4.7,
            reviewCount: 145,
            stock: 40,
            isFeatured: false,
            brand: 'NextGen Fashion'
        },
        {
            id: '5',
            name: 'Premium Leather Jacket',
            description: 'Genuine leather jacket with modern design.',
            price: 149.99,
            originalPrice: 199.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1520975954732-35dd22299614?w=400'],
            localImageName: 'product_5.png',
            sizes: ['S', 'M', 'L', 'XL'],
            colors: ['Black', 'Brown', 'Tan'],
            rating: 4.9,
            reviewCount: 312,
            stock: 15,
            isFeatured: true,
            isNew: true,
            brand: 'NextGen Premium'
        },
        {
            id: '6',
            name: 'Elegant Maxi Dress',
            description: 'Flowing maxi dress with elegant floral pattern.',
            price: 69.99,
            originalPrice: 89.99,
            category: 'CLOTHING',
            subCategory: 'Dresses',
            images: ['https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=400'],
            localImageName: 'product_6.jpg',
            sizes: ['XS', 'S', 'M', 'L', 'XL'],
            colors: ['Navy Floral', 'Red Floral', 'Green Floral'],
            rating: 4.7,
            reviewCount: 178,
            stock: 20,
            isFeatured: true,
            brand: 'NextGen Fashion'
        },
        {
            id: '7',
            name: 'Black Puffer Jacket',
            description: 'Warm and stylish black puffer jacket for winter.',
            price: 89.99,
            originalPrice: 119.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400'],
            localImageName: 'product_7.png',
            sizes: ['S', 'M', 'L', 'XL'],
            colors: ['Black'],
            rating: 4.6,
            reviewCount: 50,
            stock: 20,
            isFeatured: false,
            brand: 'NextGen Winter'
        },
        {
            id: '8',
            name: 'Classic Puffer',
            description: 'Classic design black puffer jacket.',
            price: 85.99,
            originalPrice: 109.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400'],
            localImageName: 'product_8.png',
            sizes: ['S', 'M', 'L', 'XL'],
            colors: ['Black'],
            rating: 4.5,
            reviewCount: 30,
            stock: 25,
            isFeatured: false,
            brand: 'NextGen Winter'
        },
        {
            id: '9',
            name: 'Dark Blue Puffer',
            description: 'Stylish dark blue puffer jacket.',
            price: 94.99,
            originalPrice: 129.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400'],
            localImageName: 'product_9.png',
            sizes: ['M', 'L', 'XL'],
            colors: ['Dark Blue'],
            rating: 4.7,
            reviewCount: 45,
            stock: 15,
            isFeatured: true,
            brand: 'NextGen Winter'
        },
        {
            id: '10',
            name: 'Formal Black Shirt',
            description: 'Elegant formal black shirt.',
            price: 49.99,
            originalPrice: 69.99,
            category: 'CLOTHING',
            subCategory: 'Shirts',
            images: ['https://images.unsplash.com/photo-1626557981101-aae6f84aa6ff?w=400'],
            localImageName: 'product_10.png',
            sizes: ['S', 'M', 'L', 'XL'],
            colors: ['Black'],
            rating: 4.8,
            reviewCount: 88,
            stock: 40,
            isFeatured: false,
            brand: 'NextGen Formal'
        },
        {
            id: '11',
            name: 'Premium White Shirt',
            description: 'High quality premium white formal shirt.',
            price: 54.99,
            originalPrice: 74.99,
            category: 'CLOTHING',
            subCategory: 'Shirts',
            images: ['https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=400'],
            localImageName: 'product_11.png',
            sizes: ['S', 'M', 'L', 'XL'],
            colors: ['White'],
            rating: 4.9,
            reviewCount: 120,
            stock: 35,
            isFeatured: true,
            brand: 'NextGen Premium'
        },
        {
            id: '12',
            name: 'Gray Lined Shirt',
            description: 'Textured gray shirt for business casual look.',
            price: 44.99,
            originalPrice: 59.99,
            category: 'CLOTHING',
            subCategory: 'Shirts',
            images: ['https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=400'],
            localImageName: 'product_12.png',
            sizes: ['M', 'L', 'XL'],
            colors: ['Gray'],
            rating: 4.5,
            reviewCount: 60,
            stock: 30,
            isFeatured: false,
            brand: 'NextGen Formal'
        },
        {
            id: '13',
            name: 'Light Gray Formal',
            description: 'Light gray formal shirt.',
            price: 45.99,
            originalPrice: 59.99,
            category: 'CLOTHING',
            subCategory: 'Shirts',
            images: ['https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=400'],
            localImageName: 'product_13.png',
            sizes: ['S', 'M', 'L', 'XL'],
            colors: ['Light Gray'],
            rating: 4.6,
            reviewCount: 70,
            stock: 25,
            isFeatured: false,
            brand: 'NextGen Formal'
        },
        {
            id: '14',
            name: 'Navy Blue Shirt',
            description: 'Deep navy blue formal shirt.',
            price: 49.99,
            originalPrice: 69.99,
            category: 'CLOTHING',
            subCategory: 'Shirts',
            images: ['https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=400'],
            localImageName: 'product_14.png',
            sizes: ['S', 'M', 'L', 'XL'],
            colors: ['Navy'],
            rating: 4.7,
            reviewCount: 95,
            stock: 30,
            isFeatured: true,
            brand: 'NextGen Formal'
        },
        {
            id: '15',
            name: 'Pink Puffer',
            description: 'Vibrant pink puffer jacket for ladies.',
            price: 89.99,
            originalPrice: 119.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400'],
            localImageName: 'product_15.png',
            sizes: ['XS', 'S', 'M', 'L'],
            colors: ['Pink'],
            rating: 4.8,
            reviewCount: 110,
            stock: 20,
            isFeatured: true,
            brand: 'NextGen Winter'
        },
        {
            id: '16',
            name: 'Red Puffer Design',
            description: 'Modern red puffer jacket design.',
            price: 99.99,
            originalPrice: 139.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400'],
            localImageName: 'product_16.webp',
            sizes: ['S', 'M', 'L'],
            colors: ['Red'],
            rating: 4.9,
            reviewCount: 40,
            stock: 15,
            isFeatured: true,
            brand: 'NextGen Winter'
        },
        {
            id: '17',
            name: 'Red Puffer Classic',
            description: 'Classic red puffer jacket.',
            price: 89.99,
            originalPrice: 119.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400'],
            localImageName: 'product_17.png',
            sizes: ['XS', 'S', 'M', 'L'],
            colors: ['Red'],
            rating: 4.7,
            reviewCount: 85,
            stock: 25,
            isFeatured: false,
            brand: 'NextGen Winter'
        },
        {
            id: '18',
            name: 'Brown Leather Jacket',
            description: 'Stylish brown leather jacket with white collar.',
            price: 159.99,
            originalPrice: 219.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1520975954732-35dd22299614?w=400'],
            localImageName: 'product_18.png',
            sizes: ['S', 'M', 'L', 'XL'],
            colors: ['Brown'],
            rating: 4.9,
            reviewCount: 150,
            stock: 10,
            isFeatured: true,
            brand: 'NextGen Premium'
        },
        {
            id: '19',
            name: 'White Puffer',
            description: 'Clean white puffer jacket.',
            price: 99.99,
            originalPrice: 129.99,
            category: 'CLOTHING',
            subCategory: 'Jackets',
            images: ['https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400'],
            localImageName: 'product_19.png',
            sizes: ['S', 'M', 'L'],
            colors: ['White'],
            rating: 4.6,
            reviewCount: 65,
            stock: 20,
            isFeatured: false,
            brand: 'NextGen Winter'
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
app.post('/orders', requireAuth, async (req, res) => {
    try {
        const { items, shippingAddress, paymentMethod } = req.body;
        const userId = req.auth.user.id;

        if (!items || !shippingAddress || !paymentMethod) {
            return res.status(400).json({ success: false, message: 'Missing required fields' });
        }

        await assertAddressBelongsToUser(shippingAddress.id, userId);
        const pricedItems = await loadPricedOrderItems(items);
        const retailerIds = [...new Set(pricedItems.map(item => item.retailerId).filter(Boolean))];
        if (retailerIds.length > 1) {
            return res.status(400).json({
                success: false,
                message: 'Checkout currently supports one retailer per order on the backend'
            });
        }
        const retailerId = retailerIds[0] || null;
        const orderNumber = 'ORD-' + Date.now();
        const subtotal = pricedItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
        const tax = subtotal * 0.08;
        const shipping = subtotal > 5000 ? 0 : 200; // Free shipping over Rs. 5000
        const total = subtotal + tax + shipping;
        const now = Date.now();

        let newOrder = {
            id: crypto.randomUUID(),
            userId,
            orderNumber,
            items: pricedItems,
            subtotal: Math.round(subtotal),
            tax: Math.round(tax),
            shipping,
            total: Math.round(total),
            currency: 'PKR',
            status: 'PENDING',
            paymentStatus: paymentMethod === 'CASH_ON_DELIVERY' ? 'PENDING' : 'PENDING',
            paymentMethod,
            shippingAddress,
            retailerId,
            createdAt: now,
            updatedAt: now
        };

        if (supabase) {
            try {
                // Insert order into Supabase
                const { data: orderData, error: orderError } = await supabase
                    .from('orders')
                    .insert({
                        id: newOrder.id,
                        user_id: userId,
                        order_number: orderNumber,
                        subtotal: newOrder.subtotal,
                        tax: newOrder.tax,
                        shipping: newOrder.shipping,
                        total: newOrder.total,
                        currency: 'PKR',
                        status: 'PENDING',
                        payment_status: 'PENDING',
                        payment_method: paymentMethod,
                        shipping_address_id: shippingAddress.id || null,
                        retailer_id: retailerId,
                        created_at: new Date(now).toISOString(),
                        updated_at: new Date(now).toISOString()
                    })
                    .select()
                    .single();

                if (orderError) {
                    throw orderError;
                } else {
                    // Insert order items
                    const orderItems = pricedItems.map(item => ({
                        order_id: newOrder.id,
                        product_id: item.productId || '',
                        product_name: item.productName || '',
                        product_image: item.productImage || '',
                        price: item.price,
                        quantity: item.quantity,
                        selected_size: item.selectedSize || '',
                        selected_color: item.selectedColor || ''
                    }));

                    const { error: itemsError } = await supabase
                        .from('order_items')
                        .insert(orderItems);

                    if (itemsError) {
                        throw itemsError;
                    }

                    // Create payment transaction record
                    const { error: paymentError } = await supabase.from('payment_transactions').insert({
                        order_id: newOrder.id,
                        payment_method: paymentMethod,
                        transaction_id: 'TXN-' + Date.now(),
                        status: 'PENDING',
                        amount: newOrder.total,
                        currency: 'PKR',
                        otp_verified: false,
                        created_at: new Date(now).toISOString()
                    });
                    if (paymentError) throw paymentError;

                    if (paymentMethod === 'VAULT') {
                        const { error: vaultError } = await supabase.rpc('debit_wallet_for_order', {
                            p_user_id: userId,
                            p_amount: newOrder.total,
                            p_description: `Order payment ${orderNumber}`
                        });

                        if (vaultError) {
                            await supabase
                                .from('payment_transactions')
                                .update({ status: 'FAILED' })
                                .eq('order_id', newOrder.id);
                            await supabase
                                .from('orders')
                                .update({
                                    status: 'CANCELLED',
                                    payment_status: 'FAILED',
                                    updated_at: new Date().toISOString()
                                })
                                .eq('id', newOrder.id);
                            throw vaultError;
                        }

                        await supabase
                            .from('payment_transactions')
                            .update({
                                status: 'COMPLETED',
                                completed_at: new Date().toISOString()
                            })
                            .eq('order_id', newOrder.id);
                        await supabase
                            .from('orders')
                            .update({
                                payment_status: 'COMPLETED',
                                updated_at: new Date().toISOString()
                            })
                            .eq('id', newOrder.id);

                        newOrder.paymentStatus = 'COMPLETED';
                    }

                    console.log('✅ Order saved to Supabase:', newOrder.id);
                }
            } catch (dbErr) {
                console.error('DB error:', dbErr.message);
                return res.status(500).json({ success: false, message: 'Failed to save order' });
            }
        }

        res.json({ success: true, order: newOrder });
    } catch (error) {
        console.error('Create order error:', error.message);
        res.status(error.statusCode || 500).json({ success: false, message: 'Failed to create order', error: error.message });
    }
});

app.get('/orders/user/:userId', requireAuth, requireSameUserOrAdmin, async (req, res) => {
    const { userId } = req.params;
    try {
        if (supabase) {
            const { data, error } = await supabase
                .from('orders')
                .select(`
                    *,
                    order_items (*)
                `)
                .eq('user_id', userId)
                .order('created_at', { ascending: false });

            if (error) throw error;

            const orders = (data || []).map(row => ({
                id: row.id,
                userId: row.user_id,
                orderNumber: row.order_number,
                items: (row.order_items || []).map(i => ({
                    productId: i.product_id,
                    productName: i.product_name,
                    productImage: i.product_image,
                    price: i.price,
                    quantity: i.quantity,
                    selectedSize: i.selected_size,
                    selectedColor: i.selected_color
                })),
                subtotal: row.subtotal,
                tax: row.tax,
                shipping: row.shipping,
                total: row.total,
                currency: row.currency,
                status: row.status,
                paymentStatus: row.payment_status,
                paymentMethod: row.payment_method,
                retailerId: row.retailer_id,
                parentOrderId: row.parent_order_id,
                createdAt: new Date(row.created_at).getTime(),
                updatedAt: new Date(row.updated_at).getTime()
            }));

            return res.json({ success: true, orders });
        }
        res.json({ success: true, orders: [] });
    } catch (error) {
        console.error('Get user orders error:', error.message);
        res.status(500).json({ success: false, message: 'Failed to fetch orders', orders: [] });
    }
});

app.get('/orders/:id', requireAuth, async (req, res) => {
    const { id } = req.params;
    try {
        if (supabase) {
            const { data, error } = await supabase
                .from('orders')
                .select(`*, order_items (*)`)
                .eq('id', id)
                .single();

            if (error) throw error;
            if (data.user_id !== req.auth.user.id && !(await isAdminUser(req.auth.user.id))) {
                return res.status(403).json({ success: false, message: 'Access denied' });
            }

            const order = {
                id: data.id,
                userId: data.user_id,
                orderNumber: data.order_number,
                items: (data.order_items || []).map(i => ({
                    productId: i.product_id,
                    productName: i.product_name,
                    productImage: i.product_image,
                    price: i.price,
                    quantity: i.quantity,
                    selectedSize: i.selected_size,
                    selectedColor: i.selected_color
                })),
                subtotal: data.subtotal,
                tax: data.tax,
                shipping: data.shipping,
                total: data.total,
                currency: data.currency,
                status: data.status,
                paymentStatus: data.payment_status,
                paymentMethod: data.payment_method,
                retailerId: data.retailer_id,
                parentOrderId: data.parent_order_id,
                createdAt: new Date(data.created_at).getTime(),
                updatedAt: new Date(data.updated_at).getTime()
            };

            return res.json({ success: true, order });
        }
        res.json({ success: true, order: { id, status: 'PENDING', items: [], total: 0 } });
    } catch (error) {
        console.error('Get order error:', error.message);
        res.status(500).json({ success: false, message: 'Failed to fetch order' });
    }
});

app.put('/orders/:id/status', requireAuth, requireAdmin, async (req, res) => {
    const { id } = req.params;
    const { status } = req.body;
    try {
        if (!VALID_ORDER_STATUSES.has(status)) {
            return res.status(400).json({ success: false, message: 'Invalid order status' });
        }
        if (supabase) {
            const { error } = await supabase
                .from('orders')
                .update({ status, updated_at: new Date().toISOString() })
                .eq('id', id);
            if (error) throw error;
        }
        res.json({ success: true, message: 'Order status updated' });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Failed to update order status' });
    }
});

/**
 * Payment Transaction Endpoints
 */
app.get('/orders/:id/payment/status', requireAuth, async (req, res) => {
    try {
        const { data: order, error } = await supabase
            .from('orders')
            .select('id,user_id,payment_status,payment_method')
            .eq('id', req.params.id)
            .single();

        if (error || !order) {
            return res.status(404).json({ success: false, message: 'Order not found' });
        }

        if (order.user_id !== req.auth.user.id && !(await isAdminUser(req.auth.user.id))) {
            return res.status(403).json({ success: false, message: 'Access denied' });
        }

        return res.json({
            success: true,
            paymentStatus: order.payment_status,
            paymentMethod: order.payment_method
        });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Failed to fetch payment status' });
    }
});

app.post('/orders/:id/payment/verify', requireAuth, requireAdmin, async (req, res) => {
    const { id } = req.params;
    const { mobileNumber, otpVerified } = req.body;
    try {
        if (!otpVerified) {
            return res.status(400).json({ success: false, message: 'otpVerified must be true' });
        }
        if (supabase && otpVerified) {
            const { error: transactionError } = await supabase
                .from('payment_transactions')
                .update({
                    status: 'COMPLETED',
                    mobile_number: mobileNumber,
                    otp_verified: true,
                    completed_at: new Date().toISOString()
                })
                .eq('order_id', id);
            if (transactionError) throw transactionError;

            const { error: orderError } = await supabase
                .from('orders')
                .update({ payment_status: 'COMPLETED', updated_at: new Date().toISOString() })
                .eq('id', id);
            if (orderError) throw orderError;
        }
        res.json({ success: true, message: 'Payment verified' });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Payment verification failed' });
    }
});

/**
 * Addresses API Endpoints
 */
app.get('/addresses/:userId', requireAuth, requireSameUserOrAdmin, async (req, res) => {
    const { userId } = req.params;
    try {
        if (supabase) {
            const { data, error } = await supabase
                .from('shipping_addresses')
                .select('*')
                .eq('user_id', userId)
                .order('is_default', { ascending: false });

            if (error) throw error;

            const addresses = (data || []).map(row => ({
                id: row.id,
                userId: row.user_id,
                label: row.label,
                fullName: row.full_name,
                phone: row.phone,
                addressLine1: row.address_line1,
                addressLine2: row.address_line2 || '',
                city: row.city,
                province: row.province || '',
                postalCode: row.postal_code || '',
                country: row.country,
                isDefault: row.is_default
            }));
            return res.json({ success: true, addresses });
        }
        res.json({ success: true, addresses: [] });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Failed to fetch addresses', addresses: [] });
    }
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

app.delete('/auth/account', requireAuth, async (req, res) => {
    const userId = req.auth.user.id;

    if (!supabase) {
        return res.status(503).json({ success: false, message: 'Database is not configured' });
    }

    try {
        await supabase.from('user_fcm_tokens').delete().eq('user_id', userId);
        await supabase.from('shipping_addresses').delete().eq('user_id', userId);

        const { error: profileError } = await supabase
            .from('users')
            .update({
                email: `deleted-${userId}@deleted.local`,
                name: 'Deleted user',
                phone: '',
                profile_image_url: null,
                role: 'customer',
                updated_at: new Date().toISOString()
            })
            .eq('id', userId);

        if (profileError) throw profileError;

        const { error: authError } = await supabase.auth.admin.deleteUser(userId);
        if (authError) throw authError;

        return res.json({ success: true, message: 'Account deleted' });
    } catch (error) {
        console.error('Delete account error:', error.message);
        return res.status(500).json({ success: false, message: 'Failed to delete account' });
    }
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
 * ════════════════════════════════════════════════════════════════════════════
 * SAFEPAY PAYMENT ENDPOINTS
 * ════════════════════════════════════════════════════════════════════════════
 */

/**
 * POST /api/safepay/session
 * Creates a Safepay payment tracker server-side (keeps secret key off device).
 * Body: { orderId: String, currency?: String }
 */
app.post('/api/safepay/session', requireAuth, async (req, res) => {
    const { orderId, currency = 'PKR' } = req.body;

    if (!orderId) {
        return res.status(400).json({ success: false, message: 'orderId is required' });
    }

    try {
        const { data: order, error: orderError } = await supabase
            .from('orders')
            .select('id,user_id,total,payment_method,payment_status')
            .eq('id', orderId)
            .single();

        if (orderError || !order) {
            return res.status(404).json({ success: false, message: 'Order not found' });
        }

        if (order.user_id !== req.auth.user.id && !(await isAdminUser(req.auth.user.id))) {
            return res.status(403).json({ success: false, message: 'Access denied' });
        }

        if (order.payment_method !== 'SAFEPAY') {
            return res.status(400).json({ success: false, message: 'Order is not configured for Safepay' });
        }

        if (order.payment_status === 'COMPLETED') {
            return res.status(409).json({ success: false, message: 'Order is already paid' });
        }

        const payment = await createSafepayPayment(Number(order.total), currency);
        const tracker = payment?.token;
        if (!tracker) {
            return res.status(502).json({ success: false, message: 'Safepay did not return a tracker token' });
        }

        const checkoutUrl = createSafepayCheckoutUrl({
            token: tracker,
            orderId
        });

        // Save tracker to payment_transactions if Supabase is configured
        if (supabase) {
            const { error: transactionError } = await supabase
                .from('payment_transactions')
                .upsert({
                    order_id:        orderId,
                    safepay_tracker: tracker,
                    status:          'PENDING',
                    gateway:         'SAFEPAY'
                }, { onConflict: 'order_id' });
            if (transactionError) throw transactionError;

            // Also store tracker on the order row for quick lookup
            const { error: updateOrderError } = await supabase
                .from('orders')
                .update({ safepay_tracker: tracker })
                .eq('id', orderId);
            if (updateOrderError) throw updateOrderError;
        }

        return res.json({ success: true, tracker, checkoutUrl });

    } catch (err) {
        console.error('[Safepay] Session creation error:', err?.response?.data || err.message);
        return res.status(500).json({
            success: false,
            message: err?.response?.data?.status?.message || 'Failed to create Safepay session'
        });
    }
});

app.post('/api/safepay/webhook', async (req, res) => {
    if (!supabase) {
        return res.status(503).json({ success: false, message: 'Database not configured' });
    }

    try {
        if (!verifySafepayWebhook(req)) {
            return res.status(400).json({ success: false, message: 'Invalid Safepay signature' });
        }

        const event = req.body;
        const eventType = event?.type;
        const tracker = extractSafepayTracker(event);
        let orderId = extractSafepayOrderId(event);

        if (!orderId && tracker) {
            const { data: transactions, error: transactionLookupError } = await supabase
                .from('payment_transactions')
                .select('order_id')
                .eq('safepay_tracker', tracker)
                .limit(1);

            if (transactionLookupError) throw transactionLookupError;
            orderId = transactions?.[0]?.order_id || null;
        }

        if (!orderId) {
            return res.status(422).json({ success: false, message: 'Webhook could not be matched to an order' });
        }

        const { data: order, error: orderLookupError } = await supabase
            .from('orders')
            .select('id,payment_method,safepay_tracker')
            .eq('id', orderId)
            .single();

        if (orderLookupError || !order) {
            return res.status(404).json({ success: false, message: 'Order not found' });
        }

        if (order.payment_method !== 'SAFEPAY') {
            return res.status(400).json({ success: false, message: 'Order is not a Safepay order' });
        }

        if (tracker && order.safepay_tracker && tracker !== order.safepay_tracker) {
            return res.status(400).json({ success: false, message: 'Safepay tracker mismatch' });
        }

        const now = new Date().toISOString();
        if (eventType === 'payment.succeeded') {
            const { error: orderUpdateError } = await supabase
                .from('orders')
                .update({ payment_status: 'COMPLETED', updated_at: now })
                .eq('id', orderId);
            if (orderUpdateError) throw orderUpdateError;

            const { error: transactionUpdateError } = await supabase
                .from('payment_transactions')
                .update({
                    status: 'COMPLETED',
                    completed_at: now,
                    ...(tracker ? { safepay_tracker: tracker } : {})
                })
                .eq('order_id', orderId);
            if (transactionUpdateError) throw transactionUpdateError;
        } else if (eventType === 'payment.failed') {
            const { error: orderUpdateError } = await supabase
                .from('orders')
                .update({ payment_status: 'FAILED', updated_at: now })
                .eq('id', orderId);
            if (orderUpdateError) throw orderUpdateError;

            const { error: transactionUpdateError } = await supabase
                .from('payment_transactions')
                .update({ status: 'FAILED' })
                .eq('order_id', orderId);
            if (transactionUpdateError) throw transactionUpdateError;
        }

        return res.status(200).json({ success: true });
    } catch (error) {
        console.error('[SafepayWebhook] Error:', error.message);
        return res.status(500).json({ success: false, message: 'Webhook processing failed' });
    }
});

/**
 * PUT /orders/:id/payment/complete
 * Called by the app after Safepay payment success deep-link.
 * Marks payment_transactions as COMPLETED and order payment_status as COMPLETED.
 * Body: { tracker?: String }
 */
app.put('/orders/:id/payment/complete', requireAuth, requireAdmin, async (req, res) => {
    const { id } = req.params;
    const { tracker } = req.body;

    if (!supabase) {
        return res.status(503).json({ success: false, message: 'Database not configured' });
    }

    try {
        const now = new Date().toISOString();

        // Update orders table
        const { error: orderErr } = await supabase
            .from('orders')
            .update({
                payment_status: 'COMPLETED',
                updated_at:     now
            })
            .eq('id', id);

        if (orderErr) throw orderErr;

        // Update payment_transactions table
        const txUpdate = {
            status:       'COMPLETED',
            completed_at: now
        };
        if (tracker) txUpdate.safepay_tracker = tracker;

        const { error: transactionError } = await supabase
            .from('payment_transactions')
            .update(txUpdate)
            .eq('order_id', id);
        if (transactionError) throw transactionError;

        return res.json({ success: true, message: 'Payment marked as completed' });

    } catch (err) {
        console.error('[PaymentComplete] Error:', err.message);
        return res.status(500).json({ success: false, message: err.message });
    }
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
    console.log(`💳 Safepay Session: POST /api/safepay/session`);
    console.log(`✅ Payment Complete: PUT /orders/:id/payment/complete`);
    console.log(`💚 Health Check: GET /health`);
    console.log('='.repeat(60));
    console.log('📝 Ready to receive requests!');
    console.log('⚠️  First try-on request may take 30-60 seconds');
    console.log('='.repeat(60) + '\n');
});
