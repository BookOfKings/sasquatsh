/**
 * Image compression and resizing utilities
 */

export interface CompressOptions {
  maxSize?: number      // Max dimension in pixels (default: 400 for avatars, 800 for logos)
  quality?: number      // JPEG quality 0-1 (default: 0.85)
  minSizeToCompress?: number  // Only compress if file > this size in bytes (default: 100KB)
}

/**
 * Compress and resize an image before upload
 * - Resizes to fit within maxSize x maxSize while maintaining aspect ratio
 * - Converts to JPEG with specified quality
 * - Only compresses if file exceeds minSizeToCompress
 */
export async function compressImage(
  file: File,
  options: CompressOptions = {}
): Promise<File> {
  const {
    maxSize = 400,
    quality = 0.85,
    minSizeToCompress = 100 * 1024, // 100KB
  } = options

  // Skip compression for small files
  if (file.size <= minSizeToCompress) {
    return file
  }

  return new Promise((resolve, reject) => {
    const img = new Image()
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')

    img.onload = () => {
      // Calculate new dimensions maintaining aspect ratio
      let { width, height } = img
      if (width > height) {
        if (width > maxSize) {
          height = Math.round((height * maxSize) / width)
          width = maxSize
        }
      } else {
        if (height > maxSize) {
          width = Math.round((width * maxSize) / height)
          height = maxSize
        }
      }

      canvas.width = width
      canvas.height = height

      // Draw resized image
      ctx?.drawImage(img, 0, 0, width, height)

      // Convert to blob with compression
      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error('Failed to compress image'))
            return
          }
          // Create new file with compressed data
          const compressedFile = new File([blob], file.name, {
            type: 'image/jpeg',
            lastModified: Date.now(),
          })
          resolve(compressedFile)
        },
        'image/jpeg',
        quality
      )
    }

    img.onerror = () => reject(new Error('Failed to load image'))
    img.src = URL.createObjectURL(file)
  })
}

/**
 * Compress an avatar image (400x400 max, 85% quality)
 */
export async function compressAvatar(file: File): Promise<File> {
  return compressImage(file, {
    maxSize: 400,
    quality: 0.85,
    minSizeToCompress: 100 * 1024,
  })
}

/**
 * Compress a logo image (800x800 max, 85% quality)
 * Larger than avatars since logos may need more detail
 */
export async function compressLogo(file: File): Promise<File> {
  return compressImage(file, {
    maxSize: 800,
    quality: 0.85,
    minSizeToCompress: 100 * 1024,
  })
}
