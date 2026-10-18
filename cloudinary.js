// Cloudinary Configuration and Upload Helper for Web / JavaScript
const CLOUDINARY_URL = 'https://api.cloudinary.com/v1_1/jggtvymn/image/upload';
const UPLOAD_PRESET = 'DdFr-TgDz8NTwz91psJSuvP1i5c';
const API_KEY = '865291324538569';
const API_SECRET = 'DdFr-TgDz8NTwz91psJSuvP1i5c';
const CLOUD_NAME = 'jggtvymn';

/**
 * Upload an image file, blob, or base64 data to Cloudinary
 * Supports both unsigned upload (via UPLOAD_PRESET) and signed upload fallback
 * @param {File|Blob|string} file - Image file, blob, or base64 string
 * @returns {Promise<string>} Secure URL of the uploaded image
 */
async function uploadToCloudinary(file) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('upload_preset', UPLOAD_PRESET);

  try {
    const response = await fetch(CLOUDINARY_URL, {
      method: 'POST',
      body: formData
    });
    const result = await response.json();
    if (result.secure_url) {
      return result.secure_url;
    }
    // If unsigned preset is not configured in dashboard, fallback to signed upload
    return await uploadSigned(file);
  } catch (err) {
    return await uploadSigned(file);
  }
}

/**
 * Fallback signed upload to Cloudinary
 * @param {File|Blob|string} file
 * @returns {Promise<string>}
 */
async function uploadSigned(file) {
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const encoder = new TextEncoder();
  const data = encoder.encode(`timestamp=${timestamp}${API_SECRET}`);
  const hashBuffer = await crypto.subtle.digest('SHA-1', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const signature = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

  const formData = new FormData();
  formData.append('file', file);
  formData.append('api_key', API_KEY);
  formData.append('timestamp', timestamp);
  formData.append('signature', signature);

  const response = await fetch(CLOUDINARY_URL, {
    method: 'POST',
    body: formData
  });
  const result = await response.json();
  if (result.secure_url) {
    return result.secure_url;
  }
  throw new Error(result.error ? result.error.message : 'Signed upload failed');
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    CLOUDINARY_URL,
    UPLOAD_PRESET,
    API_KEY,
    API_SECRET,
    CLOUD_NAME,
    uploadToCloudinary
  };
}
