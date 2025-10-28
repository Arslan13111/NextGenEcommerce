// server.js
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const { GoogleGenerativeAI } = require('@google/generative-ai');
require('dotenv').config();

// Initialize Express App
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware setup
app.use(cors());
app.use(bodyParser.json({ limit: '50mb' })); // Increased limit for large base64 images
app.use(bodyParser.urlencoded({ limit: '50mb', extended: true }));

// Initialize Google Generative AI
// NOTE: Make sure your GEMINI_API_KEY is correctly set in your .env file
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

/**
 * Helper function to extract mime type and clean base64 data
 * @param {string} base64Data The full base64 string (e.g., 'data:image/jpeg;base64,...')
 * @returns {{mimeType: string, cleanData: string}}
 */
function extractMimeAndData(base64Data) {
    const parts = base64Data.match(/^data:(.+?);base64,(.*)$/);
    if (parts && parts.length === 3) {
        return {
            mimeType: parts[1],
            cleanData: parts[2].replace(/\s/g, '') // Remove any whitespace
        };
    }
    // Fallback if data URI scheme is not present
    return {
        mimeType: 'image/jpeg', // Defaulting to JPEG if scheme is missing
        cleanData: base64Data.replace(/^data:image\/\w+;base64,/, '').replace(/\s/g, '')
    };
}

/**
 * Utility function to create a delay.
 * @param {number} ms Milliseconds to wait.
 */
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Calls the Gemini API with exponential backoff for 429 (Rate Limit) errors.
 * @param {object} model The GenerativeModel instance.
 * @param {object} request The request payload for generateContent.
 * @param {number} maxRetries Maximum number of retry attempts.
 * @returns {Promise<object>} The successful response object from the API.
 */
async function callGeminiApiWithRetry(model, request, maxRetries = 3) {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            const result = await model.generateContent(request);
            return result; // Success!
        } catch (error) {
            // Check for a 429 status (Too Many Requests)
            if (error.status === 429 && attempt < maxRetries) {
                const backoffTime = Math.pow(2, attempt) * 1000; // 2s, 4s, 8s...
                console.log(`Rate limit hit (429). Retrying in ${backoffTime / 1000} seconds (Attempt ${attempt}/${maxRetries}).`);
                await delay(backoffTime);
            } else {
                // Re-throw if it's not a 429, or if we've reached max retries
                throw error;
            }
        }
    }
}


// API route for Virtual Try-On
app.post('/api/try-on', async (req, res) => {
    try {
        const { image, productName, productColor, productType } = req.body;
        
        if (!image) {
            return res.status(400).json({ 
                success: false, 
                message: 'Image is required' 
            });
        }

        console.log('Processing try-on request...');
        console.log('Product:', productName, productColor, productType);

        // Switched to gemini-2.5-flash-image-preview for image generation/editing.
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash-image-preview" });

        const prompt = `Generate a photorealistic visualization of the person in the image wearing a ${productColor} ${productName}. 
        
CRITICAL REQUIREMENTS:
1. Keep the person's face, skin tone, hair, and body structure EXACTLY as shown - DO NOT modify these.
2. Keep all other clothing items that are NOT being replaced - only replace the specific ${productType}.
3. The new ${productColor} ${productName} must look naturally worn by this person, matching lighting, shadows, and perspective.
4. Ensure the clothing fits the person's body shape realistically.
5. Maintain the original background and environment.
6. Preserve any accessories (jewelry, watches, bags) that aren't being replaced.
        
Visualize ONLY the change of the ${productType} to the new ${productColor} ${productName}, maintaining photorealistic quality.`;

        
        // Clean and prepare the base64 image data
        const { mimeType, cleanData } = extractMimeAndData(image);
        
        // Convert base64 to proper format for Gemini
        const imagePart = {
            inlineData: {
                data: cleanData,
                mimeType: mimeType 
            }
        };

        const apiRequest = {
            // Pass the prompt and the original image as input
            contents: [{ parts: [{ text: prompt }, imagePart] }],
            generationConfig: { 
                temperature: 0.2 
            }
        };

        console.log(`Sending image generation request to Gemini AI with MIME Type: ${mimeType}...`);

        // Use the retry mechanism for the API call
        const result = await callGeminiApiWithRetry(model, apiRequest);

        // Extract the generated image data from the response
        const generatedCandidate = result.candidates?.[0];

        if (!generatedCandidate || !generatedCandidate.content || !generatedCandidate.content.parts) {
             throw new Error("AI response was unsuccessful or did not contain image data.");
        }

        const generatedImagePart = generatedCandidate.content.parts.find(
            (part) => part.inlineData && part.inlineData.mimeType.startsWith('image/')
        );

        if (!generatedImagePart) {
            throw new Error("AI successfully responded but did not return a generated image.");
        }
        
        const generatedData = generatedImagePart.inlineData.data;
        const generatedMimeType = generatedImagePart.inlineData.mimeType;
        
        // Construct the full data URI for the modified image
        const resultImage = `data:${generatedMimeType};base64,${generatedData}`;

        console.log('AI Modified Image received successfully');

        // Return the modified image data (resultImage)
        res.json({
            success: true,
            resultImage: resultImage, // This is the new, modified image data
            description: `Virtual try-on visualization created successfully for ${productColor} ${productName}.`,
            message: 'Virtual try-on image generated successfully'
        });

    } catch (error) {
        console.error('Error in try-on API:');
        console.error('Error message:', error.message);
        // Do not expose error details unless debugging is enabled
        res.status(500).json({ 
            success: false, 
            message: 'Failed to process virtual try-on. This may be due to a quota limit or an invalid image.',
            error: error.message
        });
    }
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'OK', message: 'Server is running' });
});

// Start the server
app.listen(PORT, () => {
    console.log(`✅ Next Gen Ecommerce Backend running on port ${PORT}`);
    console.log(`🔗 API endpoint: http://localhost:${PORT}/api/try-on`);
});
