import Foundation

enum AppConfig {
    static let supabaseFunctionsURL = "https://yyfukoddeyiaxiufztdx.supabase.co/functions/v1"
    static let supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl5ZnVrb2RkZXlpYXhpdWZ6dGR4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE5MTAxODIsImV4cCI6MjA4NzQ4NjE4Mn0.aujhFFlmiN_rswvYK4-yMrcuiCSa5osg-0i2aINvOYw"
    static let appScheme = "sasquatsh"
    static let webDomain = "sasquatsh.com"
    static let pricingURL = URL(string: "https://\(webDomain)/pricing")!
    static let billingURL = URL(string: "https://\(webDomain)/billing")!
}
