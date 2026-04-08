package com.indie.shiftledger.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DashboardSnapshot(
    val weekRevenue: Double = 0.0,
    val weekProfit: Double = 0.0,
    val weekCosts: Double = 0.0,
    val weekHours: Double = 0.0,
    val averageJobValue: Double = 0.0,
    val monthRevenue: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val followUpCount: Int = 0,
    val topClient: String = "No jobs yet",
    val trend: List<WeeklyRevenue> = emptyList(),
) {
    companion object {
        fun fromJobs(
            jobs: List<JobRecord>,
            today: LocalDate = LocalDate.now(),
        ): DashboardSnapshot {
            if (jobs.isEmpty()) {
                return DashboardSnapshot(
                    trend = buildTrend(today, emptyList()),
                )
            }

            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val startOfMonthWindow = today.minusDays(29)

            val weekJobs = jobs.filter { !it.date.isBefore(startOfWeek) }
            val monthJobs = jobs.filter { !it.date.isBefore(startOfMonthWindow) }
            val topClient = jobs
                .groupBy { it.clientName }
                .maxByOrNull { (_, clientJobs) -> clientJobs.sumOf { it.invoiceTotal } }
                ?.key
                ?: "No jobs yet"

            val weekHours = weekJobs.sumOf { it.durationHours }
            val monthRevenue = monthJobs.sumOf { it.invoiceTotal }

            return DashboardSnapshot(
                weekRevenue = weekJobs.sumOf { it.invoiceTotal },
                weekProfit = weekJobs.sumOf { it.estimatedProfit },
                weekCosts = weekJobs.sumOf { it.totalCosts },
                weekHours = weekHours,
                averageJobValue = if (monthJobs.isEmpty()) 0.0 else monthRevenue / monthJobs.size,
                monthRevenue = monthRevenue,
                unpaidAmount = jobs
                    .filter { it.invoiceStatus.isOutstanding }
                    .sumOf { it.invoiceTotal },
                followUpCount = jobs.count { it.invoiceStatus.isOutstanding },
                topClient = topClient,
                trend = buildTrend(today, jobs),
            )
        }

        private fun buildTrend(today: LocalDate, jobs: List<JobRecord>): List<WeeklyRevenue> {
            val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            return (3 downTo 0).map { weekOffset ->
                val start = currentWeekStart.minusWeeks(weekOffset.toLong())
                val end = start.plusDays(6)
                val revenue = jobs
                    .filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
                    .sumOf { it.invoiceTotal }

                WeeklyRevenue(
                    label = if (weekOffset == 0) "Now" else "${weekOffset}w",
                    value = revenue,
                )
            }
        }
    }
}

data class WeeklyRevenue(
    val label: String,
    val value: Double,
)
