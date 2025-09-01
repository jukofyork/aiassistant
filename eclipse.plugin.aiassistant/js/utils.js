/**
 * Decodes a base64 string that contains UTF-8 encoded content.
 * This properly handles Unicode characters that would be corrupted by atob() alone.
 * 
 * @param {string} str - Base64 encoded UTF-8 string
 * @returns {string} - Decoded Unicode string
 */
function decodeBase64UTF8(str) {
    // Decode base64 to bytes, then decode bytes as UTF-8
    const bytes = Uint8Array.from(atob(str), c => c.charCodeAt(0));
    return new TextDecoder().decode(bytes);
}