import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders } from '../_shared/firebase.ts'
import { sendEmail, contactNotificationEmail } from '../_shared/email.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const ADMIN_EMAIL = Deno.env.get('ADMIN_EMAIL') || 'admin@sasquatsh.com'
const recaptchaSecretKey = Deno.env.get('RECAPTCHA_SECRET_KEY')

async function verifyRecaptcha(token: string): Promise<{ success: boolean; score?: number; error?: string }> {
  if (!recaptchaSecretKey) {
    console.warn('RECAPTCHA_SECRET_KEY not configured, skipping verification')
    return { success: true }
  }
  try {
    const response = await fetch('https://www.google.com/recaptcha/api/siteverify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `secret=${recaptchaSecretKey}&response=${token}`,
    })
    const data = await response.json()
    if (!data.success) {
      console.error('reCAPTCHA verification failed:', data)
      return { success: false, error: 'reCAPTCHA verification failed' }
    }
    // For v3, check the score (0.0 - 1.0, higher is more likely human)
    // Reject if score is below 0.3 (likely bot)
    if (data.score !== undefined && data.score < 0.3) {
      console.warn('reCAPTCHA score too low:', data.score)
      return { success: false, score: data.score, error: 'Request blocked due to suspicious activity' }
    }
    return { success: true, score: data.score }
  } catch (err) {
    console.error('reCAPTCHA verification error:', err)
    return { success: false, error: 'Failed to verify reCAPTCHA' }
  }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  if (req.method !== 'POST') {
    return errorResponse('Method not allowed', 405)
  }

  try {
    const body = await req.json()
    const { name, email, subject, message, userId, recaptchaToken } = body

    // Verify reCAPTCHA
    if (recaptchaToken) {
      const recaptchaResult = await verifyRecaptcha(recaptchaToken)
      if (!recaptchaResult.success) {
        return errorResponse(recaptchaResult.error || 'reCAPTCHA verification failed', 400)
      }
    } else if (recaptchaSecretKey) {
      // If reCAPTCHA is configured but no token provided, reject
      return errorResponse('reCAPTCHA token required', 400)
    }

    // Validation
    if (!name?.trim()) {
      return errorResponse('Name is required', 400)
    }
    if (!email?.trim()) {
      return errorResponse('Email is required', 400)
    }
    if (!subject?.trim()) {
      return errorResponse('Subject is required', 400)
    }
    if (!message?.trim()) {
      return errorResponse('Message is required', 400)
    }
    if (message.trim().length < 10) {
      return errorResponse('Message must be at least 10 characters', 400)
    }
    if (message.trim().length > 5000) {
      return errorResponse('Message must be less than 5000 characters', 400)
    }

    // Basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email.trim())) {
      return errorResponse('Please enter a valid email address', 400)
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // Store the contact submission
    const { error } = await supabase
      .from('contact_submissions')
      .insert({
        name: name.trim(),
        email: email.trim().toLowerCase(),
        subject: subject.trim(),
        message: message.trim(),
        user_id: userId || null,
        ip_address: req.headers.get('x-forwarded-for') || req.headers.get('cf-connecting-ip') || null,
        user_agent: req.headers.get('user-agent') || null,
      })

    if (error) {
      console.error('Failed to save contact submission:', error)
      return errorResponse('Failed to send message. Please try again.', 500)
    }

    // Send notification email to admin
    const emailContent = contactNotificationEmail({
      name: name.trim(),
      email: email.trim(),
      subject: subject.trim(),
      message: message.trim(),
    })

    sendEmail({
      to: ADMIN_EMAIL,
      ...emailContent,
    }).then(result => {
      if (!result.success) {
        console.error('Failed to send contact notification email:', result.error)
      }
    })

    return jsonResponse({ message: 'Message sent successfully' }, 201)
  } catch (err) {
    console.error('Contact submission error:', err)
    return errorResponse('Failed to process request', 500)
  }
})
