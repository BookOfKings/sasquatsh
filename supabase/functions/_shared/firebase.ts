// Firebase token verification for Edge Functions
// Verifies Firebase ID tokens using Google's public keys

interface DecodedToken {
  uid: string
  email?: string
  name?: string
  picture?: string
}

interface FirebaseTokenPayload {
  iss: string
  aud: string
  auth_time: number
  user_id: string
  sub: string
  iat: number
  exp: number
  email?: string
  email_verified?: boolean
  name?: string
  picture?: string
  firebase: {
    identities: Record<string, unknown>
    sign_in_provider: string
  }
}

const FIREBASE_PROJECT_ID = 'sasquatsh'

// Cache for Google's public keys
let cachedKeys: Record<string, CryptoKey> | null = null
let cacheExpiry = 0

async function getGooglePublicKeys(): Promise<Record<string, CryptoKey>> {
  const now = Date.now()
  if (cachedKeys && now < cacheExpiry) {
    return cachedKeys
  }

  const response = await fetch(
    'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com'
  )

  if (!response.ok) {
    throw new Error('Failed to fetch Google public keys')
  }

  // Get cache expiry from headers
  const cacheControl = response.headers.get('cache-control')
  const maxAgeMatch = cacheControl?.match(/max-age=(\d+)/)
  const maxAge = maxAgeMatch ? parseInt(maxAgeMatch[1], 10) * 1000 : 3600000
  cacheExpiry = now + maxAge

  const keys = await response.json() as Record<string, string>
  cachedKeys = {}

  for (const [kid, cert] of Object.entries(keys)) {
    // Extract the public key from the certificate
    const pemContents = cert
      .replace('-----BEGIN CERTIFICATE-----', '')
      .replace('-----END CERTIFICATE-----', '')
      .replace(/\s/g, '')

    const binaryDer = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0))

    // Import as X.509 certificate and extract public key
    const cryptoKey = await crypto.subtle.importKey(
      'spki',
      extractPublicKeyFromCert(binaryDer),
      { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
      false,
      ['verify']
    )
    cachedKeys[kid] = cryptoKey
  }

  return cachedKeys
}

// Extract public key bytes from X.509 certificate
function extractPublicKeyFromCert(certDer: Uint8Array): ArrayBuffer {
  // This is a simplified extraction - in production you might use a proper ASN.1 parser
  // The public key in X.509 is typically found after specific sequences
  // For Firebase/Google certs, we can use a known offset pattern

  // Find the SubjectPublicKeyInfo sequence
  // Look for sequence starting with 30 82 (SEQUENCE with 2-byte length)
  let offset = 0

  // Skip outer SEQUENCE
  if (certDer[offset] !== 0x30) throw new Error('Invalid certificate format')
  offset++
  if (certDer[offset] & 0x80) {
    const lenBytes = certDer[offset] & 0x7f
    offset += 1 + lenBytes
  } else {
    offset++
  }

  // Skip tbsCertificate SEQUENCE header
  if (certDer[offset] !== 0x30) throw new Error('Invalid tbsCertificate')
  offset++
  if (certDer[offset] & 0x80) {
    const lenBytes = certDer[offset] & 0x7f
    offset += 1 + lenBytes
  } else {
    offset++
  }

  // Skip version (if present - context tag [0])
  if (certDer[offset] === 0xa0) {
    offset++
    const len = certDer[offset++]
    offset += len
  }

  // Skip serialNumber (INTEGER)
  if (certDer[offset] === 0x02) {
    offset++
    let len = certDer[offset++]
    if (len & 0x80) {
      const lenBytes = len & 0x7f
      len = 0
      for (let i = 0; i < lenBytes; i++) {
        len = (len << 8) | certDer[offset++]
      }
    }
    offset += len
  }

  // Skip signature algorithm (SEQUENCE)
  if (certDer[offset] === 0x30) {
    offset++
    let len = certDer[offset++]
    if (len & 0x80) {
      const lenBytes = len & 0x7f
      len = 0
      for (let i = 0; i < lenBytes; i++) {
        len = (len << 8) | certDer[offset++]
      }
    }
    offset += len
  }

  // Skip issuer (SEQUENCE)
  if (certDer[offset] === 0x30) {
    offset++
    let len = certDer[offset++]
    if (len & 0x80) {
      const lenBytes = len & 0x7f
      len = 0
      for (let i = 0; i < lenBytes; i++) {
        len = (len << 8) | certDer[offset++]
      }
    }
    offset += len
  }

  // Skip validity (SEQUENCE)
  if (certDer[offset] === 0x30) {
    offset++
    let len = certDer[offset++]
    if (len & 0x80) {
      const lenBytes = len & 0x7f
      len = 0
      for (let i = 0; i < lenBytes; i++) {
        len = (len << 8) | certDer[offset++]
      }
    }
    offset += len
  }

  // Skip subject (SEQUENCE)
  if (certDer[offset] === 0x30) {
    offset++
    let len = certDer[offset++]
    if (len & 0x80) {
      const lenBytes = len & 0x7f
      len = 0
      for (let i = 0; i < lenBytes; i++) {
        len = (len << 8) | certDer[offset++]
      }
    }
    offset += len
  }

  // Now we should be at SubjectPublicKeyInfo (SEQUENCE)
  if (certDer[offset] !== 0x30) {
    throw new Error('Could not find SubjectPublicKeyInfo')
  }

  const spkiStart = offset
  offset++
  let spkiLen = certDer[offset++]
  if (spkiLen & 0x80) {
    const lenBytes = spkiLen & 0x7f
    spkiLen = 0
    for (let i = 0; i < lenBytes; i++) {
      spkiLen = (spkiLen << 8) | certDer[offset++]
    }
  }

  // Return the full SubjectPublicKeyInfo
  const headerLen = offset - spkiStart
  return certDer.slice(spkiStart, spkiStart + headerLen + spkiLen).buffer
}

function base64UrlDecode(str: string): Uint8Array {
  // Add padding if needed
  const pad = str.length % 4
  if (pad) {
    str += '='.repeat(4 - pad)
  }
  // Replace URL-safe characters
  const base64 = str.replace(/-/g, '+').replace(/_/g, '/')
  const binary = atob(base64)
  return Uint8Array.from(binary, (c) => c.charCodeAt(0))
}

export async function verifyFirebaseToken(token: string): Promise<DecodedToken | null> {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return null
    }

    const [headerB64, payloadB64, signatureB64] = parts

    // Decode header to get key ID
    const header = JSON.parse(new TextDecoder().decode(base64UrlDecode(headerB64)))
    const kid = header.kid

    if (!kid || header.alg !== 'RS256') {
      return null
    }

    // Get public keys
    const keys = await getGooglePublicKeys()
    const publicKey = keys[kid]

    if (!publicKey) {
      return null
    }

    // Verify signature
    const data = new TextEncoder().encode(`${headerB64}.${payloadB64}`)
    const signature = base64UrlDecode(signatureB64)

    const isValid = await crypto.subtle.verify(
      'RSASSA-PKCS1-v1_5',
      publicKey,
      signature,
      data
    )

    if (!isValid) {
      return null
    }

    // Decode and validate payload
    const payload = JSON.parse(
      new TextDecoder().decode(base64UrlDecode(payloadB64))
    ) as FirebaseTokenPayload

    const now = Math.floor(Date.now() / 1000)

    // Validate claims
    if (payload.exp < now) {
      return null // Token expired
    }
    if (payload.iat > now + 300) {
      return null // Token issued in the future (with 5 min grace)
    }
    if (payload.aud !== FIREBASE_PROJECT_ID) {
      return null // Wrong audience
    }
    if (!payload.iss.startsWith('https://securetoken.google.com/')) {
      return null // Wrong issuer
    }
    if (!payload.sub || payload.sub !== payload.user_id) {
      return null // Invalid subject
    }

    return {
      uid: payload.sub,
      email: payload.email,
      name: payload.name,
      picture: payload.picture,
    }
  } catch {
    return null
  }
}

export function getCorsHeaders(): Record<string, string> {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type, x-firebase-token',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  }
}

// Get Firebase token from request headers
export function getFirebaseToken(req: Request): string | null {
  return req.headers.get('X-Firebase-Token')
}

export function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
      ...getCorsHeaders(),
    },
  })
}

export function errorResponse(message: string, status = 400): Response {
  return jsonResponse({ error: message }, status)
}
