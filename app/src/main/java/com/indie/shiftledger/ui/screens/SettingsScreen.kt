package com.indie.shiftledger.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.ThemeMode
import com.indie.shiftledger.model.formatCurrency
import com.indie.shiftledger.ui.theme.LedgerHeroPanel
import com.indie.shiftledger.ui.theme.LedgerPanel
import com.indie.shiftledger.ui.theme.LedgerPill
import com.indie.shiftledger.ui.theme.LedgerSectionHeader
import com.indie.shiftledger.ui.theme.ledgerTextFieldColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    contentPadding: PaddingValues,
    currency: CurrencyOption,
    themeMode: ThemeMode,
    companyName: String,
    logoUri: String?,
    onCurrencySelected: (CurrencyOption) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onCompanyNameChanged: (String) -> Unit,
    onLogoUriChanged: (String?) -> Unit,
) {
    val context = LocalContext.current
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        onLogoUriChanged(uri.toString())
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LedgerHeroPanel {
                LedgerPill(
                    label = "Settings",
                    containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                    contentColor = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = companyName.ifBlank { "Invoice branding" },
                    style = MaterialTheme.typography.displaySmall,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = "Choose your currency, color mode, and the branding customers see on each PDF invoice.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PreferenceHeroPill(label = currency.displayLabel)
                    PreferenceHeroPill(label = themeMode.label)
                    PreferenceHeroPill(label = if (logoUri == null) "Name only" else "Logo ready")
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Invoice branding",
                    body = "Company name and custom logo both appear on exported invoices.",
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = companyName,
                    onValueChange = onCompanyNameChanged,
                    label = { Text("Company name") },
                    placeholder = { Text("Example: Alpine Electrical") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = ledgerTextFieldColors(),
                )
                LogoPreviewCard(
                    companyName = companyName,
                    logoUri = logoUri,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            logoPicker.launch(
                                arrayOf(
                                    "image/png",
                                    "image/jpeg",
                                    "image/webp",
                                ),
                            )
                        },
                    ) {
                        Text(if (logoUri == null) "Choose logo" else "Replace logo")
                    }
                    if (logoUri != null) {
                        OutlinedButton(onClick = { onLogoUriChanged(null) }) {
                            Text("Remove logo")
                        }
                    }
                }
                Text(
                    text = "PNG, JPG, or WEBP work best. If you skip the logo, the PDF still uses your company name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Currency",
                    body = "This changes how money is shown across the app and inside the PDF.",
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Current format",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(text = currency.displayLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Sample invoice ${formatCurrency(1285.50, currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                CurrencyPicker(
                    label = "Currency",
                    selected = currency,
                    onSelect = onCurrencySelected,
                )
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Appearance",
                    body = "Use light mode or dark mode.",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = "Color mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (themeMode == ThemeMode.AmoledDark) {
                                "Dark mode uses black backgrounds and brighter text."
                            } else {
                                "Light mode uses white backgrounds and black text."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Current mode: ${themeMode.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Switch(
                        checked = themeMode == ThemeMode.AmoledDark,
                        onCheckedChange = { enabled ->
                            onThemeModeChanged(
                                if (enabled) ThemeMode.AmoledDark else ThemeMode.Light,
                            )
                        },
                    )
                }
            }
        }

        item {
            LedgerPanel {
                LedgerSectionHeader(
                    title = "Preview",
                    body = "A quick look at how the PDF details will read.",
                )
                Text(
                    text = companyName.ifBlank { "No company name added yet" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(text = "Revenue ${formatCurrency(3210.40, currency)}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Costs ${formatCurrency(685.10, currency)}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Profit ${formatCurrency(2525.30, currency)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (logoUri == null) {
                        "The invoice header will use text only."
                    } else {
                        "The invoice header will include the selected logo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LogoPreviewCard(
    companyName: String,
    logoUri: String?,
) {
    val context = LocalContext.current
    val preview by produceState(
        initialValue = LogoPreviewState(),
        key1 = logoUri,
    ) {
        value = withContext(Dispatchers.IO) {
            loadLogoPreview(context, logoUri)
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (preview.bitmap != null) {
                Image(
                    bitmap = preview.bitmap!!.asImageBitmap(),
                    contentDescription = "Selected logo",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = companyName.ifBlank { "Your company name" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = preview.displayName ?: if (logoUri == null) {
                        "No logo selected. PDF invoices will use text only."
                    } else {
                        "Logo selected for PDF invoices."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PreferenceHeroPill(
    label: String,
) {
    LedgerPill(
        label = label,
        containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
        contentColor = androidx.compose.ui.graphics.Color.White,
    )
}

private data class LogoPreviewState(
    val bitmap: Bitmap? = null,
    val displayName: String? = null,
)

private fun loadLogoPreview(
    context: Context,
    logoUri: String?,
): LogoPreviewState {
    val parsedUri = logoUri?.let(Uri::parse) ?: return LogoPreviewState()
    val displayName = resolveDocumentName(context, parsedUri)
    val bitmap = runCatching {
        context.contentResolver.openInputStream(parsedUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }.getOrNull()?.scaledToFit(maxEdge = 240)

    return LogoPreviewState(
        bitmap = bitmap,
        displayName = displayName,
    )
}

private fun resolveDocumentName(
    context: Context,
    uri: Uri,
): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }
    }.getOrNull() ?: uri.lastPathSegment
}

private fun Bitmap.scaledToFit(
    maxEdge: Int,
): Bitmap {
    if (width <= maxEdge && height <= maxEdge) return this

    val scale = minOf(maxEdge / width.toFloat(), maxEdge / height.toFloat())
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
}
