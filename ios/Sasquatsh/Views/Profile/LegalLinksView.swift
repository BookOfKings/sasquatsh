import SwiftUI

struct LegalLinksView: View {
    var body: some View {
        VStack(spacing: 8) {
            legalLink("Terms of Service", url: "https://sasquatsh.com/terms", icon: "doc.text")
            legalLink("Privacy Policy", url: "https://sasquatsh.com/privacy", icon: "lock.shield")
            legalLink("Contact Us", url: "https://sasquatsh.com/contact", icon: "envelope")
        }
    }

    private func legalLink(_ title: String, url: String, icon: String) -> some View {
        Link(destination: URL(string: url)!) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .frame(width: 20)
                Text(title)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 11))
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            .padding(.vertical, 6)
        }
    }
}
