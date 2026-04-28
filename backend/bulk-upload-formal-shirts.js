const { createClient } = require('@supabase/supabase-js');
const fs = require('fs');
const path = require('path');

const SUPABASE_URL = 'https://ccrscwaixfmfglylcjpj.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNjcnNjd2FpeGZtZmdseWxjanBqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI4OTcwMTEsImV4cCI6MjA3ODQ3MzAxMX0.175rDjZRCH4v7_UTw-43q3aXkjrzLNoDPl3avWsVAcE';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const PRICING = {
    'formal black': 5200,
    'formal white shirt': 5000,
    'gray lined texture': 5400,
    'light gray formal shirt': 5100,
    'navy blue': 5300,
};

const DEFAULT_PRICE = 5000;

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
    const { data, error } = await supabase.from('products').select('name');
    if (error) return [];
    return data.map(p => p.name.toLowerCase());
}

function extractColorFromName(productName) {
    const colors = [];
    const lowerName = productName.toLowerCase();
    if (lowerName.includes('black')) colors.push('Black');
    if (lowerName.includes('white')) colors.push('White');
    if (lowerName.includes('gray') || lowerName.includes('grey')) colors.push('Gray');
    if (lowerName.includes('navy')) colors.push('Navy');
    if (lowerName.includes('blue')) colors.push('Blue');
    return colors.length > 0 ? colors : ['Mixed'];
}

async function bulkUpload() {
    const sourceDir = path.join(__dirname, '..', 'Formal Shirts');

    console.log('='.repeat(60));
    console.log('BULK PRODUCT UPLOAD - FORMAL SHIRTS');
    console.log('='.repeat(60));
    console.log(`\nSource folder: ${sourceDir}\n`);

    const files = fs.readdirSync(sourceDir).filter(f =>
        ['.png', '.jpg', '.jpeg', '.webp'].includes(path.extname(f).toLowerCase())
    );

    console.log(`Found ${files.length} images to process\n`);

    const existingProducts = await getExistingProducts();
    console.log(`Found ${existingProducts.length} existing products in database\n`);

    const results = { uploaded: [], skipped: [], failed: [] };

    for (const file of files) {
        const productName = getProductName(file);
        const lowerProductName = productName.toLowerCase();

        console.log(`Processing: ${productName}`);

        if (existingProducts.includes(lowerProductName)) {
            console.log(`  → SKIPPED (already exists)\n`);
            results.skipped.push({ name: productName, reason: 'Already exists' });
            continue;
        }

        const imagePath = path.join(sourceDir, file);
        const price = getPrice(productName);

        console.log(`  Category: Men Formal Shirts`);
        console.log(`  Gender: male`);
        console.log(`  Price: ${price}`);

        try {
            const imageBuffer = fs.readFileSync(imagePath);
            const ext = path.extname(file).toLowerCase();
            const storageFileName = `${productName.toLowerCase().replace(/\s+/g, '-')}-${Date.now()}${ext}`;

            const { error: uploadError } = await supabase.storage
                .from('product-images')
                .upload(storageFileName, imageBuffer, {
                    contentType: getContentType(file),
                    upsert: false
                });

            if (uploadError) {
                console.log(`  → FAILED (upload: ${uploadError.message})\n`);
                results.failed.push({ name: productName, error: uploadError.message });
                continue;
            }

            const { data: urlData } = supabase.storage.from('product-images').getPublicUrl(storageFileName);

            const { data: productData, error: productError } = await supabase
                .from('products')
                .insert({
                    name: productName,
                    price: price,
                    description: "Men's formal shirt",
                    image_url: urlData.publicUrl,
                    category: 'Men Formal Shirts',
                    sub_category: 'Formal Wear',
                    sizes: ['S', 'M', 'L', 'XL', 'XXL'],
                    colors: extractColorFromName(productName),
                    in_stock: true,
                    is_new: true,
                    stock: 25,
                    brand: 'NextGen Fashion',
                    tags: ['male', 'formal', 'shirt', 'office']
                })
                .select();

            if (productError) {
                console.log(`  → FAILED (insert: ${productError.message})\n`);
                results.failed.push({ name: productName, error: productError.message });
                continue;
            }

            console.log(`  → SUCCESS\n`);
            results.uploaded.push({ name: productName, id: productData[0].id, price: price });

        } catch (err) {
            console.log(`  → FAILED (${err.message})\n`);
            results.failed.push({ name: productName, error: err.message });
        }
    }

    console.log('\n' + '='.repeat(60));
    console.log('UPLOAD SUMMARY');
    console.log('='.repeat(60));

    console.log(`\n✓ Successfully uploaded: ${results.uploaded.length}`);
    if (results.uploaded.length > 0) {
        console.log('\nUploaded products:');
        results.uploaded.forEach(p => console.log(`  - ${p.name} | Men Formal Shirts | male | ₹${p.price}`));
    }

    console.log(`\n⊘ Skipped (duplicates): ${results.skipped.length}`);
    results.skipped.forEach(p => console.log(`  - ${p.name} (${p.reason})`));

    console.log(`\n✗ Failed: ${results.failed.length}`);
    results.failed.forEach(p => console.log(`  - ${p.name}: ${p.error}`));

    console.log('\n' + '='.repeat(60));
    console.log(`Total processed: ${files.length}`);
    console.log('='.repeat(60));
}

bulkUpload().catch(console.error);
