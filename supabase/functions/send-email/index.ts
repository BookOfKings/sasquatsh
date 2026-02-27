import { jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY')
const FROM_EMAIL = Deno.env.get('FROM_EMAIL') || 'GameNight <noreply@sasquatsh.com>'

interface EmailRequest {
  to: string
  subject: string
  html: string
  text?: string
}

async function sendEmail(email: EmailRequest): Promise<{ id: string }> {
  if (!RESEND_API_KEY) {
    throw new Error('RESEND_API_KEY not configured')
  }

  const response = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${RESEND_API_KEY}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      from: FROM_EMAIL,
      to: email.to,
      subject: email.subject,
      html: email.html,
      text: email.text,
    }),
  })

  if (!response.ok) {
    const error = await response.json()
    throw new Error(error.message || 'Failed to send email')
  }

  return response.json()
}

// Email templates
function welcomeEmailHtml(displayName: string, username: string): string {
  return `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Welcome to GameNight!</title>
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="text-align: center; margin-bottom: 30px;">
    <h1 style="color: #6366f1; margin-bottom: 10px;">Welcome to GameNight!</h1>
  </div>

  <p>Hey ${displayName || username},</p>

  <p>Thanks for joining GameNight! We're excited to have you as part of our community of board game enthusiasts.</p>

  <p>Here's what you can do:</p>
  <ul>
    <li><strong>Find Games</strong> - Browse events near you or at conventions</li>
    <li><strong>Host Events</strong> - Create your own game nights and invite players</li>
    <li><strong>Join Groups</strong> - Connect with local gaming communities</li>
    <li><strong>Plan Sessions</strong> - Coordinate with friends to find the perfect game</li>
  </ul>

  <div style="text-align: center; margin: 30px 0;">
    <a href="https://sasquatsh.web.app/dashboard" style="background-color: #6366f1; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-weight: 600;">Go to Dashboard</a>
  </div>

  <p>Happy gaming!</p>
  <p style="color: #666;">— The GameNight Team</p>

  <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
  <p style="font-size: 12px; color: #999; text-align: center;">
    You're receiving this because you signed up for GameNight.<br>
    <a href="https://sasquatsh.web.app" style="color: #6366f1;">sasquatsh.web.app</a>
  </p>
</body>
</html>
`
}

function welcomeEmailText(displayName: string, username: string): string {
  return `
Welcome to GameNight!

Hey ${displayName || username},

Thanks for joining GameNight! We're excited to have you as part of our community of board game enthusiasts.

Here's what you can do:
- Find Games - Browse events near you or at conventions
- Host Events - Create your own game nights and invite players
- Join Groups - Connect with local gaming communities
- Plan Sessions - Coordinate with friends to find the perfect game

Get started: https://sasquatsh.web.app/dashboard

Happy gaming!
— The GameNight Team
`
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  if (req.method !== 'POST') {
    return errorResponse('Method not allowed', 405)
  }

  try {
    const body = await req.json()
    const { type, to, displayName, username } = body

    // Internal calls from other functions use a secret key
    const internalKey = req.headers.get('X-Internal-Key')
    const isInternalCall = internalKey === Deno.env.get('INTERNAL_SERVICE_KEY')

    // External calls require Firebase auth and admin privileges
    if (!isInternalCall) {
      const token = getFirebaseToken(req)
      if (!token) {
        return errorResponse('Unauthorized', 401)
      }
      const firebaseUser = await verifyFirebaseToken(token)
      if (!firebaseUser) {
        return errorResponse('Invalid token', 401)
      }
      // For now, only allow internal calls for sending emails
      return errorResponse('Not authorized to send emails', 403)
    }

    if (!to) {
      return errorResponse('Recipient email required', 400)
    }

    let result: { id: string }

    switch (type) {
      case 'welcome':
        result = await sendEmail({
          to,
          subject: 'Welcome to GameNight!',
          html: welcomeEmailHtml(displayName || '', username || ''),
          text: welcomeEmailText(displayName || '', username || ''),
        })
        break

      default:
        return errorResponse('Unknown email type', 400)
    }

    return jsonResponse({ success: true, emailId: result.id })
  } catch (err) {
    console.error('Email error:', err)
    return errorResponse(err instanceof Error ? err.message : 'Failed to send email', 500)
  }
})
