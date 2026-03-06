import SwiftUI

struct InvoiceDetailSheet: View {
    let invoice: Invoice
    let viewModel: BillingViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Status & Date
                    VStack(spacing: 8) {
                        invoiceStatusBadge(invoice.status)

                        Text(viewModel.formattedDate(invoice.invoiceDate))
                            .font(.md3HeadlineMedium)
                    }
                    .padding(.top)

                    // Amount
                    VStack(spacing: 4) {
                        Text(viewModel.formattedAmount(invoice.amountCents))
                            .font(.md3HeadlineLarge)
                            .foregroundStyle(Color.md3Primary)

                        Text(invoice.currency.uppercased())
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    .padding()
                    .frame(maxWidth: .infinity)
                    .cardStyle()

                    // Details
                    VStack(spacing: 12) {
                        if let start = invoice.periodStart, let end = invoice.periodEnd {
                            detailRow(label: "Period", value: "\(viewModel.formattedDate(start)) - \(viewModel.formattedDate(end))")
                        }

                        if let brand = invoice.paymentMethodBrand, let last4 = invoice.paymentMethodLast4 {
                            detailRow(label: "Payment Method", value: "\(viewModel.formatCardBrand(brand)) ••••\(last4)")
                        }

                        if let taxCents = invoice.taxCents, taxCents > 0 {
                            detailRow(label: "Tax", value: viewModel.formattedAmount(taxCents))
                        }

                        detailRow(label: "Total", value: viewModel.formattedAmount(invoice.amountCents))
                    }
                    .padding()
                    .cardStyle()

                    // Actions
                    VStack(spacing: 12) {
                        if let urlString = invoice.hostedInvoiceUrl, let url = URL(string: urlString) {
                            Button {
                                openURL(url)
                            } label: {
                                Label("View Receipt", systemImage: "doc.text")
                                    .outlinedButtonStyle()
                            }
                        }

                        if let urlString = invoice.invoicePdfUrl, let url = URL(string: urlString) {
                            Button {
                                openURL(url)
                            } label: {
                                Label("Download PDF", systemImage: "arrow.down.doc")
                                    .outlinedButtonStyle()
                            }
                        }
                    }
                }
                .padding()
            }
            .background(Color.md3SurfaceContainer)
            .navigationTitle("Invoice Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private func detailRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3OnSurfaceVariant)
            Spacer()
            Text(value)
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3OnSurface)
        }
    }

    private func invoiceStatusBadge(_ status: InvoiceStatus) -> some View {
        let (text, color): (String, Color) = switch status {
        case .paid: ("Paid", .md3PrimaryContainer)
        case .open: ("Open", .md3TertiaryContainer)
        case .draft: ("Draft", .md3SurfaceContainerHigh)
        case .void: ("Void", .md3ErrorContainer)
        case .uncollectible: ("Uncollectible", .md3ErrorContainer)
        }
        return BadgeView(text: text, color: color)
    }
}
