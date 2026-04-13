const { createClient } = require('@supabase/supabase-js');
const fs = require('fs');
const path = require('path');

const SUPABASE_URL = 'https://ccrscwaixfmfglylcjpj.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNjcnNjd2FpeGZtZmdseWxjanBqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI4OTcwMTEsImV4cCI6MjA3ODQ3MzAxMX0.175rDjZRCH4v7_UTw-43q3aXkjrzLNoDPl3avWsVAcE';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// Pricing rules (case-insensitive matching)
const PRICING = {
    'black puffer jacket': 6000,
    'leather brown': 7500,
    'red puffer jacket': 6500,
    'dark blue puffer': 5800,
    'pink female puffer': 6200,
    'pink female pufeer': 6200, // Handle typo in filename
    'red puffer female design 2': 6400,
    'white puffer': 5900,
};

// Men's jackets (case-insensitive matching)
const MENS_JACKETS = [
    'leather brown',
    'black puffer jacket',
    'red puffer jacket'
];

// Default price for items not in the pricing list
const DEFAULT_PRICE = 5500;

function getProductName(filename) {
    // Remove extension
    return path.basename(filename, path.extname(filename));
}

function getPrice(productName) {
    const lowerName = productName.toLowerCase();
    for (const [key, price] of Object.entries(PRICING)) {
        if (lowerName === key || lowerName.includes(key)) {
            return price;
        }
    }
    return DEFAULT_PRICE;
}

function getCategoryAndGender(productName) {
    const lowerName = productName.toLowerCase();
    for (const menItem of MENS_JACKETS) {
        if (lowerName === menItem || lowerName.includes(menItem)) {
            return { category: 'Men Jackets', gender: 'male' };
        }
    }
    return { category: 'Women Jackets', gender: 'female' };
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

async function bulkUpload() {
    const jacketsDir = path.join(__dirname, '..', 'Female Jackets');

    console.log('='.repeat(60));
    console.log('BULK PRODUCT UPLOAD');
    console.log('='.repeat(60));
    console.log(`\nSource folder: ${jacketsDir}\n`);

    // Get list of image files
    const files = fs.readdirSync(jacketsDir).filter(f =>
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

        const imagePath = path.join(jacketsDir, file);
        const { category, gender } = getCategoryAndGender(productName);
        const price = getPrice(productName);
        const contentType = getContentType(file);

        console.log(`  Category: ${category}`);
        console.log(`  Gender: ${gender}`);
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
                    description: 'Winter jacket',
                    image_url: imageUrl,
                    category: category,
                    sub_category: gender === 'male' ? 'Men Outerwear' : 'Women Outerwear',
                    sizes: ['S', 'M', 'L', 'XL'],
                    colors: extractColorFromName(productName),
                    in_stock: true,
                    is_new: true,
                    stock: 15,
                    brand: 'NextGen Fashion',
                    tags: [gender, 'jacket', 'winter', 'puffer']
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
                category: category,
                gender: gender,
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
            console.log(`  - ${p.name} | ${p.category} | ${p.gender} | ₹${p.price}`);
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

function extractColorFromName(productName) {
    const colors = [];
    const lowerName = productName.toLowerCase();

    if (lowerName.includes('black')) colors.push('Black');
    if (lowerName.includes('brown')) colors.push('Brown');
    if (lowerName.includes('red')) colors.push('Red');
    if (lowerName.includes('blue')) colors.push('Blue');
    if (lowerName.includes('pink')) colors.push('Pink');
    if (lowerName.includes('white')) colors.push('White');
    if (lowerName.includes('dark')) colors.push('Dark');

    return colors.length > 0 ? colors : ['Mixed'];
}

bulkUpload().catch(console.error);
