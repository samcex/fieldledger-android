package com.indie.shiftledger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.model.InvoiceStatus

@Composable
fun InvoiceStatusBadge(
    status: InvoiceStatus,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (status) {
        InvoiceStatus.DraftQuote -> MaterialTheme.colorScheme.tertiaryContainer
        InvoiceStatus.InvoiceSent -> MaterialTheme.colorScheme.secondaryContainer
        InvoiceStatus.Paid -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (status) {
        InvoiceStatus.DraftQuote -> MaterialTheme.colorScheme.onTertiaryContainer
        InvoiceStatus.InvoiceSent -> MaterialTheme.colorScheme.onSecondaryContainer
        InvoiceStatus.Paid -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier.wrapContentWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Text(
            text = status.label.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
