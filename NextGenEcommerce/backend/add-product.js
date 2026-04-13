const { createClient } = require('@supabase/supabase-js');
const fs = require('fs');
const path = require('path');

const SUPABASE_URL = 'https://ccrscwaixfmfglylcjpj.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNjcnNjd2FpeGZtZmdseWxjanBqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI4OTcwMTEsImV4cCI6MjA3ODQ3MzAxMX0.175rDjZRCH4v7_UTw-43q3aXkjrzLNoDPl3avWsVAcE';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

async function addProduct() {
    const productName = 'Female Jacket';
    const productPrice = 6000;
    const productDescription = 'Winter female puffer jacket';
    const imagePath = path.join(__dirname, '..', 'Female Jackets', 'black puffer.png');

    console.log('Reading image from:', imagePath);

    // Read the image file
    const imageBuffer = fs.readFileSync(imagePath);
    const fileName = `female-jacket-${Date.now()}.png`;

    console.log('Uploading image to Supabase storage bucket: product-images...');

    // Upload image to storage
    const { data: uploadData, error: uploadError } = await supabase.storage
        .from('product-images')
        .upload(fileName, imageBuffer, {
            contentType: 'image/png',
            upsert: false
        });

    if (uploadError) {
        console.error('Upload error:', uploadError);
        console.log('\nIf you see a permission error, make sure:');
        console.log('1. The bucket "product-images" exists and is PUBLIC');
        console.log('2. Storage policies allow anonymous uploads (or authenticate first)');
        return;
    }

    console.log('Image uploaded successfully:', uploadData);

    // Get public URL
    const { data: urlData } = supabase.storage
        .from('product-images')
        .getPublicUrl(fileName);

    const imageUrl = urlData.publicUrl;
    console.log('Public URL:', imageUrl);

    // Insert product into database
    console.log('Inserting product into database...');

    const { data: productData, error: productError } = await supabase
        .from('products')
        .insert({
            name: productName,
            price: productPrice,
            description: productDescription,
            image_url: imageUrl,
            category: 'CLOTHING',
            sub_category: 'Jackets',
            sizes: ['S', 'M', 'L', 'XL'],
            colors: ['Black'],
            in_stock: true,
            is_new: true,
            stock: 10,
            brand: 'NextGen Fashion'
        })
        .select();

    if (productError) {
        console.error('Insert error:', productError);
        console.log('\nIf you see "relation products does not exist":');
        console.log('Run the SQL script: supabase_products_setup.sql in Supabase SQL Editor');
        return;
    }

    console.log('\n✓ Product added successfully!');
    console.log('Product details:', JSON.stringify(productData, null, 2));
}

addProduct().catch(console.error);
