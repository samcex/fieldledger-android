package com.indie.shiftledger.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DashboardSnapshotTest {
    @Test
    fun calculatesWeeklyRevenueOutstandingAmountAndTopClient() {
        val today = LocalDate.of(2026, 4, 8)
        val jobs = listOf(
            JobRecord(
                id = 1,
                clientName = "Acme Bakery",
                jobName = "Grease trap service",
                siteAddress = "12 Market St",
                date = LocalDate.of(2026, 4, 7),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(13, 0),
                laborRate = 85.0,
                materialsBilled = 120.0,
                calloutFee = 40.0,
                extraCharge = 0.0,
                materialsCost = 70.0,
                travelCost = 20.0,
                invoiceStatus = InvoiceStatus.InvoiceSent,
                workSummary = "",
            ),
            JobRecord(
                id = 2,
                clientName = "Northside Dental",
                jobName = "Drain line inspection",
                siteAddress = "88 Cedar Ave",
                date = LocalDate.of(2026, 4, 6),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(13, 0),
                laborRate = 90.0,
                materialsBilled = 0.0,
                calloutFee = 0.0,
                extraCharge = 60.0,
                materialsCost = 25.0,
                travelCost = 10.0,
                invoiceStatus = InvoiceStatus.Paid,
                workSummary = "",
            ),
        )

        val snapshot = DashboardSnapshot.fromJobs(jobs, today)

        assertEquals(830.0, snapshot.weekRevenue, 0.01)
        assertEquals(705.0, snapshot.weekProfit, 0.01)
        assertEquals(500.0, snapshot.unpaidAmount, 0.01)
        assertEquals("Acme Bakery", snapshot.topClient)
    }
}
