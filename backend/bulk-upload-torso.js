const { createClient } = require('@supabase/supabase-js');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_ANON_KEY;

if (!SUPABASE_URL || !SUPABASE_KEY) {
    throw new Error('SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY or SUPABASE_ANON_KEY must be configured');
}

const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

// Pricing rules
const PRICING = {
    'green female torso': 4200,
    'torso 1': 4000,
    'torso 2': 4500,
};

const DEFAULT_PRICE = 4000;

function getProductName(filename) {
    return path.basename(filename, path.extname(filename));
}

function getPrice(productName) {
    const lowerName = productName.toLowerCase();
    return PRICING[lowerName] || DEFAULT_PRICE;
}

function getContentType(filename) {
    const ext = path.extname(filename).toLowerCase();
    switch (ext) {
        case '.png': return 'image/png';
        case '.jpg':
        case '.jpeg': return 'image/jpeg';
        case '.webp': return 'image/webp';
        default: return 'image/png';
    }
}

async function getExistingProducts() {
    const { data, error } = await supabase
        .from('products')
        .select('name');

    if (error) {
        console.error('Error fetching existing products:', error);
        return [];
    }
    return data.map(p => p.name.toLowerCase());
}

function extractColorFromName(productName) {
    const colors = [];
    const lowerName = productName.toLowerCase();

    if (lowerName.includes('green')) colors.push('Green');
    if (lowerName.includes('black')) colors.push('Black');
    if (lowerName.includes('white')) colors.push('White');
    if (lowerName.includes('red')) colors.push('Red');
    if (lowerName.includes('blue')) colors.push('Blue');
    if (lowerName.includes('pink')) colors.push('Pink');

    return colors.length > 0 ? colors : ['Mixed'];
}

async function bulkUpload() {
    const torsoDir = path.join(__dirname, '..', 'FEMALE TORSO');

    console.log('='.repeat(60));
    console.log('BULK PRODUCT UPLOAD - FEMALE TORSO');
    console.log('='.repeat(60));
    console.log(`\nSource folder: ${torsoDir}\n`);

    // Get list of image files
    const files = fs.readdirSync(torsoDir).filter(f =>
        ['.png', '.jpg', '.jpeg', '.webp'].includes(path.extname(f).toLowerCase())
    );

    console.log(`Found ${files.length} images to process\n`);

    // Get existing products to avoid duplicates
    const existingProducts = await getExistingProducts();
    console.log(`Found ${existingProducts.length} existing products in database\n`);

    const results = {
        uploaded: [],
        skipped: [],
        failed: []
    };

    for (const file of files) {
        const productName = getProductName(file);
        const lowerProductName = productName.toLowerCase();

        console.log(`Processing: ${productName}`);

        // Check for duplicates
        if (existingProducts.includes(lowerProductName)) {
            console.log(`  → SKIPPED (already exists)\n`);
            results.skipped.push({ name: productName, reason: 'Already exists' });
            continue;
        }

        const imagePath = path.join(torsoDir, file);
        const price = getPrice(productName);
        const contentType = getContentType(file);

        console.log(`  Category: Women Tops`);
        console.log(`  Gender: female`);
        console.log(`  Price: ${price}`);

        try {
            // Read and upload image
            const imageBuffer = fs.readFileSync(imagePath);
            const ext = path.extname(file).toLowerCase();
            const storageFileName = `${productName.toLowerCase().replace(/\s+/g, '-')}-${Date.now()}${ext}`;

            const { data: uploadData, error: uploadError } = await supabase.storage
                .from('product-images')
                .upload(storageFileName, imageBuffer, {
                    contentType: contentType,
                    upsert: false
                });

            if (uploadError) {
                console.log(`  → FAILED (upload error: ${uploadError.message})\n`);
                results.failed.push({ name: productName, error: uploadError.message });
                continue;
            }

            // Get public URL
            const { data: urlData } = supabase.storage
                .from('product-images')
                .getPublicUrl(storageFileName);

            const imageUrl = urlData.publicUrl;

            // Insert product into database
            const { data: productData, error: productError } = await supabase
                .from('products')
                .insert({
                    name: productName,
                    price: price,
                    description: "Women's winter torso top",
                    image_url: imageUrl,
                    category: 'Women Tops',
                    sub_category: 'Torso',
                    sizes: ['S', 'M', 'L', 'XL'],
                    colors: extractColorFromName(productName),
                    in_stock: true,
                    is_new: true,
                    stock: 20,
                    brand: 'NextGen Fashion',
                    tags: ['female', 'top', 'torso', 'winter']
                })
                .select();

            if (productError) {
                console.log(`  → FAILED (insert error: ${productError.message})\n`);
                results.failed.push({ name: productName, error: productError.message });
                continue;
            }

            console.log(`  → SUCCESS\n`);
            results.uploaded.push({
                name: productName,
                id: productData[0].id,
                price: price,
                imageUrl: imageUrl
            });

        } catch (err) {
            console.log(`  → FAILED (${err.message})\n`);
            results.failed.push({ name: productName, error: err.message });
        }
    }

    // Print summary
    console.log('\n' + '='.repeat(60));
    console.log('UPLOAD SUMMARY');
    console.log('='.repeat(60));

    console.log(`\n✓ Successfully uploaded: ${results.uploaded.length}`);
    if (results.uploaded.length > 0) {
        console.log('\nUploaded products:');
        results.uploaded.forEach(p => {
            console.log(`  - ${p.name} | Women Tops | female | ₹${p.price}`);
        });
    }

    console.log(`\n⊘ Skipped (duplicates): ${results.skipped.length}`);
    if (results.skipped.length > 0) {
        results.skipped.forEach(p => {
            console.log(`  - ${p.name} (${p.reason})`);
        });
    }

    console.log(`\n✗ Failed: ${results.failed.length}`);
    if (results.failed.length > 0) {
        results.failed.forEach(p => {
            console.log(`  - ${p.name}: ${p.error}`);
        });
    }

    console.log('\n' + '='.repeat(60));
    console.log(`Total processed: ${files.length}`);
    console.log('='.repeat(60));

    return results;
}

bulkUpload().catch(console.error);
