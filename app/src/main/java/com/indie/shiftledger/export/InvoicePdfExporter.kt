package com.indie.shiftledger.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.JobRecord
import com.indie.shiftledger.model.PricingMode
import com.indie.shiftledger.model.formatCurrency
import com.indie.shiftledger.model.formatShortDate
import java.io.File

class InvoicePdfExporter(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun export(
        job: JobRecord,
        currency: CurrencyOption,
        companyName: String,
        logoUri: String?,
    ): File {
        val exportDir = File(appContext.cacheDir, "invoice-exports").apply { mkdirs() }
        val safeClient = job.clientName.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-')
        val exportFile = File(
            exportDir,
            "fieldledger-${job.id}-${safeClient.ifBlank { "invoice" }}.pdf",
        )

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val brandName = companyName.ifBlank { "ShiftLedger" }
        val logoBitmap = loadLogoBitmap(logoUri)

        val pageWidth = pageInfo.pageWidth.toFloat()
        val contentLeft = 42f
        val contentRight = pageWidth - 42f
        val contentWidth = contentRight - contentLeft

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F4F4F5")
        }
        val headerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4D4D8")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 24f
            isFakeBoldText = true
        }
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 18f
            isFakeBoldText = true
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 15f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 12f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5F6368")
            textSize = 11f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 12f
            isFakeBoldText = true
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E4E4E7")
            strokeWidth = 1f
        }

        val headerRect = RectF(contentLeft, 34f, contentRight, 150f)
        canvas.drawRoundRect(headerRect, 26f, 26f, headerPaint)
        canvas.drawRoundRect(headerRect, 26f, 26f, headerBorderPaint)

        var brandTextLeft = contentLeft + 22f
        if (logoBitmap != null) {
            val scaledLogo = logoBitmap.scaledToFit(maxWidth = 72, maxHeight = 72)
            val logoLeft = contentLeft + 20f
            val logoTop = 56f
            val logoRect = RectF(
                logoLeft,
                logoTop,
                logoLeft + scaledLogo.width,
                logoTop + scaledLogo.height,
            )
            canvas.drawBitmap(scaledLogo, null, logoRect, null)
            brandTextLeft = logoRect.right + 18f
        }

        canvas.drawText(brandName, brandTextLeft, 78f, brandPaint)
        canvas.drawText("Invoice for ${job.clientName}", brandTextLeft, 102f, bodyPaint)
        canvas.drawText("Status: ${job.invoiceStatus.label}", brandTextLeft, 122f, mutedPaint)

        val invoiceLabel = "INVOICE"
        val invoiceLabelWidth = titlePaint.measureText(invoiceLabel)
        canvas.drawText(invoiceLabel, contentRight - invoiceLabelWidth - 20f, 80f, titlePaint)
        val totalText = formatCurrency(job.invoiceTotal, currency)
        val totalWidth = accentPaint.measureText(totalText)
        canvas.drawText(totalText, contentRight - totalWidth - 20f, 118f, accentPaint)

        var y = 188f
        canvas.drawText("Job details", contentLeft, y, sectionPaint)
        y += 24f
        drawLine(canvas, bodyPaint, "Service", job.jobName, y)
        y += 20f
        drawLine(canvas, bodyPaint, "Date", formatShortDate(job.date), y)
        y += 20f
        drawLine(
            canvas,
            bodyPaint,
            if (job.pricingMode == PricingMode.Hourly) "Time" else "Pricing",
            if (job.pricingMode == PricingMode.Hourly) job.timeWindowLabel else job.pricingMode.label,
            y,
        )
        y += 20f
        drawLine(
            canvas,
            bodyPaint,
            if (job.pricingMode == PricingMode.Hourly) "Rate" else "Job price",
            if (job.pricingMode == PricingMode.Hourly) {
                "${formatCurrency(job.laborRate, currency)}/hr"
            } else {
                formatCurrency(job.fixedPrice, currency)
            },
            y,
        )
        y += 20f
        drawLine(canvas, bodyPaint, "Site", job.siteAddress.ifBlank { "Not provided" }, y)
        y += 20f
        drawLine(canvas, bodyPaint, "Due date", job.paymentDueDate?.let(::formatShortDate) ?: "Not set", y)

        y += 22f
        canvas.drawLine(contentLeft, y, contentRight, y, dividerPaint)
        y += 28f

        canvas.drawText("Financial summary", contentLeft, y, sectionPaint)
        y += 24f
        drawLine(canvas, bodyPaint, job.baseChargeLabel, formatCurrency(job.laborTotal, currency), y)
        y += 20f
        drawLine(canvas, bodyPaint, "Materials billed", formatCurrency(job.materialsBilled, currency), y)
        y += 20f
        drawLine(canvas, bodyPaint, "Callout fee", formatCurrency(job.calloutFee, currency), y)
        y += 20f
        drawLine(canvas, bodyPaint, "Extra charge", formatCurrency(job.extraCharge, currency), y)
        y += 20f
        drawLine(canvas, accentPaint, "Invoice total", formatCurrency(job.invoiceTotal, currency), y)
        y += 20f
        drawLine(canvas, bodyPaint, "Estimated costs", formatCurrency(job.totalCosts, currency), y)
        y += 20f
        drawLine(canvas, bodyPaint, "Estimated profit", formatCurrency(job.estimatedProfit, currency), y)

        y += 22f
        canvas.drawLine(contentLeft, y, contentRight, y, dividerPaint)
        y += 28f

        canvas.drawText("Work summary", contentLeft, y, sectionPaint)
        y += 24f
        wrapText(
            text = job.workSummary.ifBlank { "No work summary was provided." },
            canvasWidth = contentWidth,
            x = contentLeft,
            y = y,
            canvas = canvas,
            paint = bodyPaint,
        )

        document.finishPage(page)
        exportFile.outputStream().use(document::writeTo)
        document.close()

        return exportFile
    }

    private fun drawLine(
        canvas: Canvas,
        paint: Paint,
        label: String,
        value: String,
        y: Float,
    ) {
        canvas.drawText(label, 42f, y, paint)
        canvas.drawText(value, 220f, y, paint)
    }

    private fun wrapText(
        text: String,
        canvasWidth: Float,
        x: Float,
        y: Float,
        canvas: Canvas,
        paint: Paint,
    ) {
        val words = text.split(Regex("\\s+"))
        val lineBuilder = StringBuilder()
        var lineY = y

        words.forEach { word ->
            val candidate = if (lineBuilder.isEmpty()) {
                word
            } else {
                "${lineBuilder.toString()} $word"
            }

            if (paint.measureText(candidate) > canvasWidth && lineBuilder.isNotEmpty()) {
                canvas.drawText(lineBuilder.toString(), x, lineY, paint)
                lineBuilder.clear()
                lineBuilder.append(word)
                lineY += 16f
            } else {
                if (lineBuilder.isEmpty()) {
                    lineBuilder.append(word)
                } else {
                    lineBuilder.append(" ").append(word)
                }
            }
        }

        if (lineBuilder.isNotEmpty()) {
            canvas.drawText(lineBuilder.toString(), x, lineY, paint)
        }
    }

    private fun loadLogoBitmap(
        logoUri: String?,
    ): Bitmap? {
        val parsedUri = logoUri?.let(Uri::parse) ?: return null
        return runCatching {
            appContext.contentResolver.openInputStream(parsedUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        }.getOrNull()
    }

    private fun Bitmap.scaledToFit(
        maxWidth: Int,
        maxHeight: Int,
    ): Bitmap {
        if (width <= maxWidth && height <= maxHeight) return this

        val scale = minOf(maxWidth / width.toFloat(), maxHeight / height.toFloat())
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }
}
