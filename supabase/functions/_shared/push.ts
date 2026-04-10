// FCM Push Notification Utility
// Uses FCM HTTP v1 API with Google service account credentials

interface PushResult {
  success: boolean
  error?: string
}

// Cache the access token (valid for ~1 hour)
let cachedAccessToken: string | null = null
let tokenExpiresAt = 0

async function getAccessToken(): Promise<string | null> {
  // Return cached token if still valid
  if (cachedAccessToken && Date.now() < tokenExpiresAt - 60000) {
    return cachedAccessToken
  }

  const serviceAccountJson = Deno.env.get('FIREBASE_SERVICE_ACCOUNT')
  if (!serviceAccountJson) {
    console.error('[Push] FIREBASE_SERVICE_ACCOUNT env var not set')
    return null
  }

  try {
    const sa = JSON.parse(serviceAccountJson)

    // Create JWT for Google OAuth2
    const header = { alg: 'RS256', typ: 'JWT' }
    const now = Math.floor(Date.now() / 1000)
    const payload = {
      iss: sa.client_email,
      scope: 'https://www.googleapis.com/auth/firebase.messaging',
      aud: 'https://oauth2.googleapis.com/token',
      iat: now,
      exp: now + 3600,
    }

    const enc = (obj: unknown) =>
      btoa(JSON.stringify(obj)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')

    const unsignedToken = `${enc(header)}.${enc(payload)}`

    // Import the private key and sign
    const pemContents = sa.private_key
      .replace(/-----BEGIN PRIVATE KEY-----/, '')
      .replace(/-----END PRIVATE KEY-----/, '')
      .replace(/\s/g, '')

    const binaryKey = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0))

    const cryptoKey = await crypto.subtle.importKey(
      'pkcs8',
      binaryKey,
      { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
      false,
      ['sign']
    )

    const signature = await crypto.subtle.sign(
      'RSASSA-PKCS1-v1_5',
      cryptoKey,
      new TextEncoder().encode(unsignedToken)
    )

    const signatureB64 = btoa(String.fromCharCode(...new Uint8Array(signature)))
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_')

    const jwt = `${unsignedToken}.${signatureB64}`

    // Exchange JWT for access token
    const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
    })

    if (!tokenResponse.ok) {
      const errText = await tokenResponse.text()
      console.error('[Push] Token exchange failed:', errText)
      return null
    }

    const tokenData = await tokenResponse.json()
    cachedAccessToken = tokenData.access_token
    tokenExpiresAt = Date.now() + (tokenData.expires_in * 1000)
    return cachedAccessToken
  } catch (err) {
    console.error('[Push] Failed to get access token:', err)
    return null
  }
}

export async function sendPushNotification(
  fcmToken: string,
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<PushResult> {
  try {
    const accessToken = await getAccessToken()
    if (!accessToken) {
      return { success: false, error: 'No access token' }
    }

    const message: Record<string, unknown> = {
      token: fcmToken,
      notification: { title, body },
      apns: {
        payload: {
          aps: {
            alert: { title, body },
            sound: 'default',
            badge: 1,
          },
        },
      },
    }

    if (data) {
      message.data = data
    }

    const response = await fetch(
      'https://fcm.googleapis.com/v1/projects/sasquatsh/messages:send',
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message }),
      }
    )

    if (!response.ok) {
      const errText = await response.text()
      console.error('[Push] FCM send failed:', errText)
      return { success: false, error: errText }
    }

    return { success: true }
  } catch (err) {
    console.error('[Push] Error sending notification:', err)
    return { success: false, error: String(err) }
  }
}
