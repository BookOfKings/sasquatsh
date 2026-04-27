package com.sasquatsh.app.views.billing

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.models.Invoice
import com.sasquatsh.app.models.InvoiceStatus
import com.sasquatsh.app.viewmodels.BillingViewModel
import com.sasquatsh.app.views.shared.BadgeView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailSheet(
    invoice: Invoice,
    billingViewModel: BillingViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Invoice Details",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Status & Date
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InvoiceStatusBadge(status = invoice.status)

                Text(
                    text = billingViewModel.formattedDate(invoice.invoiceDate),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Amount
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = billingViewModel.formattedAmount(invoice.amountCents),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = invoice.currency.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Details
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val periodStart = invoice.periodStart
                    val periodEnd = invoice.periodEnd
                    if (periodStart != null && periodEnd != null) {
                        DetailRow(
                            label = "Period",
                            value = "${billingViewModel.formattedDate(periodStart)} - ${billingViewModel.formattedDate(periodEnd)}"
                        )
                    }

                    val brand = invoice.paymentMethodBrand
                    val last4 = invoice.paymentMethodLast4
                    if (brand != null && last4 != null) {
                        DetailRow(
                            label = "Payment Method",
                            value = "${billingViewModel.formatCardBrand(brand)} ****$last4"
                        )
                    }

                    val taxCents = invoice.taxCents
                    if (taxCents != null && taxCents > 0) {
                        DetailRow(
                            label = "Tax",
                            value = billingViewModel.formattedAmount(taxCents)
                        )
                    }

                    DetailRow(
                        label = "Total",
                        value = billingViewModel.formattedAmount(invoice.amountCents)
                    )
                }
            }

            // Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                invoice.hostedInvoiceUrl?.let { urlString ->
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Receipt")
                    }
                }

                invoice.invoicePdfUrl?.let { urlString ->
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download PDF")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InvoiceStatusBadge(status: InvoiceStatus) {
    val (text, color) = when (status) {
        InvoiceStatus.PAID -> "Paid" to MaterialTheme.colorScheme.primaryContainer
        InvoiceStatus.OPEN -> "Open" to MaterialTheme.colorScheme.tertiaryContainer
        InvoiceStatus.DRAFT -> "Draft" to MaterialTheme.colorScheme.surfaceContainerHigh
        InvoiceStatus.VOID -> "Void" to MaterialTheme.colorScheme.errorContainer
        InvoiceStatus.UNCOLLECTIBLE -> "Uncollectible" to MaterialTheme.colorScheme.errorContainer
    }
    BadgeView(text = text, color = color)
}
