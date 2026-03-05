// SendGrid email utility

const SENDGRID_API_KEY = Deno.env.get('SENDGRID_API_KEY')
const FROM_EMAIL = 'noreply@sasquatsh.com'
const FROM_NAME = 'Sasquatsh'

export interface EmailOptions {
  to: string
  subject: string
  text?: string
  html?: string
}

export async function sendEmail(options: EmailOptions): Promise<{ success: boolean; error?: string }> {
  if (!SENDGRID_API_KEY) {
    console.error('SENDGRID_API_KEY not configured')
    return { success: false, error: 'Email service not configured' }
  }

  try {
    const response = await fetch('https://api.sendgrid.com/v3/mail/send', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${SENDGRID_API_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        personalizations: [{ to: [{ email: options.to }] }],
        from: { email: FROM_EMAIL, name: FROM_NAME },
        subject: options.subject,
        content: [
          ...(options.text ? [{ type: 'text/plain', value: options.text }] : []),
          ...(options.html ? [{ type: 'text/html', value: options.html }] : []),
        ],
      }),
    })

    if (response.ok || response.status === 202) {
      return { success: true }
    }

    const errorText = await response.text()
    console.error('SendGrid error:', response.status, errorText)
    return { success: false, error: `SendGrid error: ${response.status}` }
  } catch (err) {
    console.error('Email send failed:', err)
    return { success: false, error: err instanceof Error ? err.message : 'Unknown error' }
  }
}

// Email templates

export function invitationEmail(params: {
  eventTitle: string
  hostName: string
  eventDate: string
  eventTime: string
  location: string
  inviteUrl: string
}): { subject: string; html: string; text: string } {
  const subject = `You're invited to play: ${params.eventTitle}`

  const text = `
You've been invited to a game night!

${params.eventTitle}
Hosted by ${params.hostName}
${params.eventDate} at ${params.eventTime}
${params.location}

Join the game: ${params.inviteUrl}

See you there!
- The Sasquatsh Team
`.trim()

  const html = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="text-align: center; margin-bottom: 30px;">
    <h1 style="color: #7c3aed; margin: 0;">Sasquatsh</h1>
    <p style="color: #666; margin: 5px 0;">You've been invited to a game night!</p>
  </div>

  <div style="background: linear-gradient(135deg, #f5f3ff 0%, #fdf4ff 100%); border-radius: 12px; padding: 24px; margin-bottom: 24px;">
    <h2 style="margin: 0 0 16px 0; color: #1f2937;">${params.eventTitle}</h2>
    <p style="margin: 8px 0; color: #4b5563;">
      <strong>Host:</strong> ${params.hostName}
    </p>
    <p style="margin: 8px 0; color: #4b5563;">
      <strong>When:</strong> ${params.eventDate} at ${params.eventTime}
    </p>
    <p style="margin: 8px 0; color: #4b5563;">
      <strong>Where:</strong> ${params.location}
    </p>
  </div>

  <div style="text-align: center; margin: 30px 0;">
    <a href="${params.inviteUrl}" style="display: inline-block; background: #7c3aed; color: white; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 600;">
      Join the Game
    </a>
  </div>

  <p style="color: #666; font-size: 14px; text-align: center;">
    See you there!<br>
    The Sasquatsh Team
  </p>

  <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 30px 0;">

  <p style="color: #9ca3af; font-size: 12px; text-align: center;">
    If you didn't expect this invitation, you can safely ignore this email.
  </p>
</body>
</html>
`.trim()

  return { subject, html, text }
}

export function contactNotificationEmail(params: {
  name: string
  email: string
  subject: string
  message: string
}): { subject: string; html: string; text: string } {
  const subject = `[Sasquatsh Contact] ${params.subject}`

  const text = `
New contact form submission:

From: ${params.name} <${params.email}>
Subject: ${params.subject}

Message:
${params.message}
`.trim()

  const html = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
  <h2 style="color: #7c3aed;">New Contact Form Submission</h2>

  <div style="background: #f9fafb; border-radius: 8px; padding: 16px; margin: 20px 0;">
    <p style="margin: 8px 0;"><strong>From:</strong> ${params.name} &lt;${params.email}&gt;</p>
    <p style="margin: 8px 0;"><strong>Subject:</strong> ${params.subject}</p>
  </div>

  <div style="background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px;">
    <p style="margin: 0; white-space: pre-wrap;">${params.message}</p>
  </div>

  <p style="color: #666; font-size: 14px; margin-top: 24px;">
    <a href="mailto:${params.email}">Reply to ${params.name}</a>
  </p>
</body>
</html>
`.trim()

  return { subject, html, text }
}

export function planningInviteEmail(params: {
  sessionTitle: string
  groupName: string
  hostName: string
  proposedDates: string[]
  responseDeadline: string
  planningUrl: string
}): { subject: string; html: string; text: string } {
  const subject = `You're invited to plan: ${params.sessionTitle}`

  const datesText = params.proposedDates.map(d => `  • ${d}`).join('\n')
  const datesHtml = params.proposedDates.map(d => `<li style="margin: 4px 0;">${d}</li>`).join('')

  const text = `
You've been invited to help Plan a Game!

${params.sessionTitle}
Group: ${params.groupName}
Organized by ${params.hostName}

Proposed dates:
${datesText}

Please respond by: ${params.responseDeadline}

Vote on dates and suggest games: ${params.planningUrl}

- The Sasquatsh Team
`.trim()

  const html = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
  <div style="text-align: center; margin-bottom: 30px;">
    <h1 style="color: #7c3aed; margin: 0;">Sasquatsh</h1>
    <p style="color: #666; margin: 5px 0;">You've been invited to help Plan a Game!</p>
  </div>

  <div style="background: linear-gradient(135deg, #f5f3ff 0%, #fdf4ff 100%); border-radius: 12px; padding: 24px; margin-bottom: 24px;">
    <h2 style="margin: 0 0 16px 0; color: #1f2937;">${params.sessionTitle}</h2>
    <p style="margin: 8px 0; color: #4b5563;">
      <strong>Group:</strong> ${params.groupName}
    </p>
    <p style="margin: 8px 0; color: #4b5563;">
      <strong>Organized by:</strong> ${params.hostName}
    </p>
    <p style="margin: 8px 0; color: #4b5563;">
      <strong>Respond by:</strong> ${params.responseDeadline}
    </p>
  </div>

  <div style="background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; margin-bottom: 24px;">
    <p style="margin: 0 0 8px 0; font-weight: 600; color: #374151;">Proposed Dates:</p>
    <ul style="margin: 0; padding-left: 20px; color: #4b5563;">
      ${datesHtml}
    </ul>
  </div>

  <div style="text-align: center; margin: 30px 0;">
    <a href="${params.planningUrl}" style="display: inline-block; background: #7c3aed; color: white; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 600;">
      Vote &amp; Suggest Games
    </a>
  </div>

  <p style="color: #666; font-size: 14px; text-align: center;">
    Let's find a time that works for everyone!<br>
    The Sasquatsh Team
  </p>

  <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 30px 0;">

  <p style="color: #9ca3af; font-size: 12px; text-align: center;">
    If you didn't expect this invitation, you can safely ignore this email.
  </p>
</body>
</html>
`.trim()

  return { subject, html, text }
}
