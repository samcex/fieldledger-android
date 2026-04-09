import { enrichJob, formatCurrency, formatShortDate, getPricingMode } from "./logic.js";

const PAGE_WIDTH = 595;
const PAGE_HEIGHT = 842;
const CANVAS_WIDTH = 1240;
const CANVAS_HEIGHT = 1754;

function safeFilenamePart(value) {
  return String(value ?? "")
    .replace(/[^A-Za-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase();
}

function createCanvas() {
  const canvas = document.createElement("canvas");
  canvas.width = CANVAS_WIDTH;
  canvas.height = CANVAS_HEIGHT;
  return canvas;
}

function drawRoundedRect(context, x, y, width, height, radius, fillStyle, strokeStyle = null) {
  context.beginPath();
  context.moveTo(x + radius, y);
  context.lineTo(x + width - radius, y);
  context.quadraticCurveTo(x + width, y, x + width, y + radius);
  context.lineTo(x + width, y + height - radius);
  context.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
  context.lineTo(x + radius, y + height);
  context.quadraticCurveTo(x, y + height, x, y + height - radius);
  context.lineTo(x, y + radius);
  context.quadraticCurveTo(x, y, x + radius, y);
  context.closePath();
  context.fillStyle = fillStyle;
  context.fill();
  if (strokeStyle) {
    context.strokeStyle = strokeStyle;
    context.lineWidth = 2;
    context.stroke();
  }
}

function wrapText(context, text, x, y, maxWidth, lineHeight) {
  const words = String(text ?? "").split(/\s+/).filter(Boolean);
  let line = "";
  let lineY = y;
  for (const word of words) {
    const candidate = line ? `${line} ${word}` : word;
    if (context.measureText(candidate).width > maxWidth && line) {
      context.fillText(line, x, lineY);
      line = word;
      lineY += lineHeight;
    } else {
      line = candidate;
    }
  }
  if (line) {
    context.fillText(line, x, lineY);
  }
  return lineY;
}

function loadImage(dataUrl) {
  if (!dataUrl) return Promise.resolve(null);
  return new Promise((resolve) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => resolve(null);
    image.src = dataUrl;
  });
}

function scaleToFit(width, height, maxWidth, maxHeight) {
  const ratio = Math.min(maxWidth / width, maxHeight / height, 1);
  return {
    width: width * ratio,
    height: height * ratio,
  };
}

async function renderInvoiceCanvas({ job, currencyCode, companyName, logoDataUrl, isPro }) {
  const enriched = enrichJob(job);
  const brandName = String(companyName ?? "").trim() || "ShiftLedger";
  const logoImage = isPro ? await loadImage(logoDataUrl) : null;
  const pricingMode = getPricingMode(enriched.pricingMode);
  const canvas = createCanvas();
  const context = canvas.getContext("2d");

  context.fillStyle = "#f5f1e8";
  context.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

  context.fillStyle = "#0f172a";
  context.globalAlpha = 0.025;
  for (let index = 0; index < 12; index += 1) {
    context.fillRect(0, 80 + index * 140, CANVAS_WIDTH, 1);
  }
  context.globalAlpha = 1;

  drawRoundedRect(context, 80, 70, CANVAS_WIDTH - 160, 290, 38, "#fffaf2", "#dfd5c3");

  let brandLeft = 130;
  if (logoImage) {
    const scaled = scaleToFit(logoImage.width, logoImage.height, 220, 220);
    drawRoundedRect(context, 112, 102, 236, 236, 30, "#ffffff");
    context.drawImage(logoImage, 120, 110, scaled.width, scaled.height);
    brandLeft = 380;
  }

  context.fillStyle = "#0f172a";
  context.font = "700 46px 'Avenir Next', 'Trebuchet MS', sans-serif";
  context.fillText(brandName, brandLeft, 170);

  context.font = "400 28px 'Avenir Next', 'Trebuchet MS', sans-serif";
  context.fillStyle = "#334155";
  context.fillText(`Invoice for ${enriched.clientName}`, brandLeft, 220);
  context.fillText(`Status: ${enriched.statusLabel}`, brandLeft, 260);

  context.font = "700 60px Georgia, 'Times New Roman', serif";
  context.fillStyle = "#0f172a";
  const invoiceLabel = "INVOICE";
  const invoiceLabelWidth = context.measureText(invoiceLabel).width;
  context.fillText(invoiceLabel, CANVAS_WIDTH - 120 - invoiceLabelWidth, 170);

  context.font = "700 36px 'Avenir Next', 'Trebuchet MS', sans-serif";
  const totalText = formatCurrency(enriched.invoiceTotal, currencyCode);
  const totalTextWidth = context.measureText(totalText).width;
  context.fillText(totalText, CANVAS_WIDTH - 120 - totalTextWidth, 255);

  const leftX = 92;
  const rightX = 760;
  let leftY = 430;
  let rightY = 430;

  context.fillStyle = "#0f172a";
  context.font = "700 30px 'Avenir Next', 'Trebuchet MS', sans-serif";
  context.fillText("Job details", leftX, leftY);
  leftY += 54;

  context.font = "400 24px 'Avenir Next', 'Trebuchet MS', sans-serif";
  const leftRows = [
    ["Service", enriched.jobName],
    ["Date", formatShortDate(enriched.date)],
    [pricingMode.storageValue === "hourly" ? "Time" : "Pricing", pricingMode.storageValue === "hourly" ? enriched.timeWindowLabel : pricingMode.label],
    [
      pricingMode.storageValue === "hourly" ? "Rate" : "Job price",
      pricingMode.storageValue === "hourly"
        ? `${formatCurrency(enriched.laborRate, currencyCode)}/hr`
        : formatCurrency(enriched.fixedPrice, currencyCode),
    ],
    ["Site", enriched.siteAddress || "Not provided"],
    ["Due date", enriched.paymentDueDate ? formatShortDate(enriched.paymentDueDate) : "Not set"],
  ];

  for (const [label, value] of leftRows) {
    context.fillStyle = "#64748b";
    context.fillText(label, leftX, leftY);
    context.fillStyle = "#0f172a";
    wrapText(context, value, leftX + 185, leftY, 420, 32);
    leftY += 48;
  }

  context.strokeStyle = "#ddd6c8";
  context.lineWidth = 2;
  context.beginPath();
  context.moveTo(80, 790);
  context.lineTo(CANVAS_WIDTH - 80, 790);
  context.stroke();

  context.fillStyle = "#0f172a";
  context.font = "700 30px 'Avenir Next', 'Trebuchet MS', sans-serif";
  context.fillText("Financial summary", rightX, rightY);
  rightY += 54;

  context.font = "400 24px 'Avenir Next', 'Trebuchet MS', sans-serif";
  const rightRows = [
    [enriched.baseChargeLabel, formatCurrency(enriched.laborTotal, currencyCode)],
    ["Materials billed", formatCurrency(enriched.materialsBilled, currencyCode)],
    ["Callout fee", formatCurrency(enriched.calloutFee, currencyCode)],
    ["Extra charge", formatCurrency(enriched.extraCharge, currencyCode)],
    ["Invoice total", formatCurrency(enriched.invoiceTotal, currencyCode)],
    ["Estimated costs", formatCurrency(enriched.totalCosts, currencyCode)],
    ["Estimated profit", formatCurrency(enriched.estimatedProfit, currencyCode)],
  ];

  for (const [label, value] of rightRows) {
    context.fillStyle = label === "Invoice total" ? "#0b8f84" : "#64748b";
    context.fillText(label, rightX, rightY);
    context.fillStyle = "#0f172a";
    context.textAlign = "right";
    context.fillText(value, CANVAS_WIDTH - 92, rightY);
    context.textAlign = "left";
    rightY += 48;
  }

  context.beginPath();
  context.moveTo(80, 1170);
  context.lineTo(CANVAS_WIDTH - 80, 1170);
  context.stroke();

  context.fillStyle = "#0f172a";
  context.font = "700 30px 'Avenir Next', 'Trebuchet MS', sans-serif";
  context.fillText("Work summary", leftX, 1240);
  context.font = "400 26px 'Avenir Next', 'Trebuchet MS', sans-serif";
  context.fillStyle = "#334155";
  wrapText(
    context,
    enriched.workSummary || "No work summary was provided.",
    leftX,
    1302,
    CANVAS_WIDTH - 184,
    40,
  );

  if (!isPro) {
    context.font = "400 22px 'Avenir Next', 'Trebuchet MS', sans-serif";
    context.fillStyle = "#64748b";
    context.textAlign = "center";
    context.fillText("Made with ShiftLedger", CANVAS_WIDTH / 2, CANVAS_HEIGHT - 55);
    context.textAlign = "left";
  }

  return canvas;
}

function concatUint8Arrays(chunks) {
  const totalLength = chunks.reduce((sum, chunk) => sum + chunk.length, 0);
  const merged = new Uint8Array(totalLength);
  let offset = 0;
  for (const chunk of chunks) {
    merged.set(chunk, offset);
    offset += chunk.length;
  }
  return merged;
}

function textBytes(value) {
  return new TextEncoder().encode(value);
}

function createPdfFromJpeg(jpegBytes, imageWidth, imageHeight) {
  const pageContent = `q\n${PAGE_WIDTH} 0 0 ${PAGE_HEIGHT} 0 0 cm\n/Im0 Do\nQ\n`;
  const objects = [
    textBytes("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"),
    textBytes("2 0 obj\n<< /Type /Pages /Count 1 /Kids [3 0 R] >>\nendobj\n"),
    textBytes(
      "3 0 obj\n" +
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
        "/Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>\n" +
        "endobj\n",
    ),
    concatUint8Arrays([
      textBytes(
        `4 0 obj\n<< /Type /XObject /Subtype /Image /Width ${imageWidth} /Height ${imageHeight} ` +
          `/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpegBytes.length} >>\nstream\n`,
      ),
      jpegBytes,
      textBytes("\nendstream\nendobj\n"),
    ]),
    concatUint8Arrays([
      textBytes(`5 0 obj\n<< /Length ${pageContent.length} >>\nstream\n${pageContent}endstream\nendobj\n`),
    ]),
  ];

  const header = textBytes("%PDF-1.4\n");
  const parts = [header];
  const offsets = [0];
  let runningLength = header.length;

  for (const objectBytes of objects) {
    offsets.push(runningLength);
    parts.push(objectBytes);
    runningLength += objectBytes.length;
  }

  const xrefOffset = runningLength;
  const xrefLines = ["xref\n0 6\n", "0000000000 65535 f \n"];
  for (let index = 1; index <= 5; index += 1) {
    xrefLines.push(`${String(offsets[index]).padStart(10, "0")} 00000 n \n`);
  }
  const trailer =
    xrefLines.join("") +
    "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" +
    `${xrefOffset}\n%%EOF`;

  parts.push(textBytes(trailer));
  return new Blob([concatUint8Arrays(parts)], { type: "application/pdf" });
}

async function canvasToJpegBytes(canvas) {
  const blob = await new Promise((resolve, reject) => {
    canvas.toBlob(
      (result) => {
        if (result) {
          resolve(result);
        } else {
          reject(new Error("Could not render invoice image."));
        }
      },
      "image/jpeg",
      0.92,
    );
  });

  const buffer = await blob.arrayBuffer();
  return new Uint8Array(buffer);
}

async function createInvoicePdfBlob(options) {
  const canvas = await renderInvoiceCanvas(options);
  const jpegBytes = await canvasToJpegBytes(canvas);
  return createPdfFromJpeg(jpegBytes, canvas.width, canvas.height);
}

async function downloadInvoicePdf(options) {
  const blob = await createInvoicePdfBlob(options);
  const enriched = enrichJob(options.job);
  const safeClient = safeFilenamePart(enriched.clientName) || "invoice";
  const fileName = `fieldledger-${enriched.id}-${safeClient}.pdf`;
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  anchor.rel = "noopener";
  anchor.click();
  setTimeout(() => URL.revokeObjectURL(url), 1500);
}

export { createInvoicePdfBlob, downloadInvoicePdf, renderInvoiceCanvas };
