package com.indie.shiftledger.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.indie.shiftledger.model.CurrencyOption
import com.indie.shiftledger.model.JobRecord
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

        val titlePaint = Paint().apply {
            textSize = 26f
            isFakeBoldText = true
        }
        val headingPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
        }
        val bodyMutedPaint = Paint().apply {
            textSize = 11f
            alpha = 180
        }

        var y = 70f
        canvas.drawText("FieldLedger Invoice", 40f, y, titlePaint)
        y += 28f
        canvas.drawText("Prepared for ${job.clientName}", 40f, y, bodyPaint)
        y += 18f
        canvas.drawText("PDF invoice generated from saved job details", 40f, y, bodyMutedPaint)

        y += 42f
        canvas.drawText("Job details", 40f, y, headingPaint)
        y += 22f
        drawLine(canvas, bodyPaint, "Service", job.jobName, y)
        y += 18f
        drawLine(canvas, bodyPaint, "Date", formatShortDate(job.date), y)
        y += 18f
        drawLine(canvas, bodyPaint, "Time", job.timeWindowLabel, y)
        y += 18f
        drawLine(canvas, bodyPaint, "Status", job.invoiceStatus.label, y)
        y += 18f
        drawLine(canvas, bodyPaint, "Site", job.siteAddress.ifBlank { "Not provided" }, y)
        y += 18f
        drawLine(canvas, bodyPaint, "Due date", job.paymentDueDate?.let(::formatShortDate) ?: "Not set", y)

        y += 36f
        canvas.drawText("Financial summary", 40f, y, headingPaint)
        y += 22f
        drawLine(canvas, bodyPaint, "Labor", formatCurrency(job.laborTotal, currency), y)
        y += 18f
        drawLine(canvas, bodyPaint, "Materials billed", formatCurrency(job.materialsBilled, currency), y)
        y += 18f
        drawLine(canvas, bodyPaint, "Callout fee", formatCurrency(job.calloutFee, currency), y)
        y += 18f
        drawLine(canvas, bodyPaint, "Extra charge", formatCurrency(job.extraCharge, currency), y)
        y += 18f
        drawLine(canvas, bodyPaint, "Invoice total", formatCurrency(job.invoiceTotal, currency), y)
        y += 18f
        drawLine(canvas, bodyPaint, "Estimated costs", formatCurrency(job.totalCosts, currency), y)
        y += 18f
        drawLine(canvas, bodyPaint, "Estimated profit", formatCurrency(job.estimatedProfit, currency), y)

        y += 36f
        canvas.drawText("Work summary", 40f, y, headingPaint)
        y += 22f
        wrapText(
            text = job.workSummary.ifBlank { "No work summary was provided." },
            canvasWidth = 515f,
            x = 40f,
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
        canvas: android.graphics.Canvas,
        paint: Paint,
        label: String,
        value: String,
        y: Float,
    ) {
        canvas.drawText(label, 40f, y, paint)
        canvas.drawText(value, 220f, y, paint)
    }

    private fun wrapText(
        text: String,
        canvasWidth: Float,
        x: Float,
        y: Float,
        canvas: android.graphics.Canvas,
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
}
