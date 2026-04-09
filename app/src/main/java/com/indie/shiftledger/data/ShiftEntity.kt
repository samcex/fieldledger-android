package com.indie.shiftledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.indie.shiftledger.model.InvoiceStatus
import com.indie.shiftledger.model.JobRecord
import com.indie.shiftledger.model.PricingMode
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val clientName: String,
    val jobName: String,
    val siteAddress: String,
    val epochDay: Long,
    val startMinute: Int,
    val endMinute: Int,
    val laborRate: Double,
    val pricingMode: String,
    val fixedPrice: Double,
    val materialsBilled: Double,
    val calloutFee: Double,
    val extraCharge: Double,
    val materialsCost: Double,
    val travelCost: Double,
    val invoiceStatus: String,
    val workSummary: String,
    val paymentDueEpochDay: Long?,
    val reminderEpochDay: Long?,
    val reminderNote: String,
)

fun JobEntity.asRecord(): JobRecord = JobRecord(
    id = id,
    clientName = clientName,
    jobName = jobName,
    siteAddress = siteAddress,
    date = LocalDate.ofEpochDay(epochDay),
    startTime = LocalTime.of(startMinute / 60, startMinute % 60),
    endTime = LocalTime.of(endMinute / 60, endMinute % 60),
    laborRate = laborRate,
    pricingMode = PricingMode.fromStorageValue(pricingMode),
    fixedPrice = fixedPrice,
    materialsBilled = materialsBilled,
    calloutFee = calloutFee,
    extraCharge = extraCharge,
    materialsCost = materialsCost,
    travelCost = travelCost,
    invoiceStatus = runCatching { InvoiceStatus.valueOf(invoiceStatus) }.getOrDefault(InvoiceStatus.DraftQuote),
    workSummary = workSummary,
    paymentDueDate = paymentDueEpochDay?.let(LocalDate::ofEpochDay),
    reminderDate = reminderEpochDay?.let(LocalDate::ofEpochDay),
    reminderNote = reminderNote,
)

fun JobRecord.asEntity(): JobEntity = JobEntity(
    id = id,
    clientName = clientName,
    jobName = jobName,
    siteAddress = siteAddress,
    epochDay = date.toEpochDay(),
    startMinute = (startTime.hour * 60) + startTime.minute,
    endMinute = (endTime.hour * 60) + endTime.minute,
    laborRate = laborRate,
    pricingMode = pricingMode.storageValue,
    fixedPrice = fixedPrice,
    materialsBilled = materialsBilled,
    calloutFee = calloutFee,
    extraCharge = extraCharge,
    materialsCost = materialsCost,
    travelCost = travelCost,
    invoiceStatus = invoiceStatus.name,
    workSummary = workSummary,
    paymentDueEpochDay = paymentDueDate?.toEpochDay(),
    reminderEpochDay = reminderDate?.toEpochDay(),
    reminderNote = reminderNote,
)
