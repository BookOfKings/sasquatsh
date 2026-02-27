import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { jsonResponse, errorResponse, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

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
    const { name, email, subject, message, userId } = body

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

    return jsonResponse({ message: 'Message sent successfully' }, 201)
  } catch (err) {
    console.error('Contact submission error:', err)
    return errorResponse('Failed to process request', 500)
  }
})
