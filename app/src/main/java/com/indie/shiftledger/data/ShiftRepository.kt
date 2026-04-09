package com.indie.shiftledger.data

import com.indie.shiftledger.model.JobRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JobRepository(
    private val jobDao: JobDao,
) {
    val jobs: Flow<List<JobRecord>> = jobDao.observeAll().map { entities ->
        entities.map { it.asRecord() }
    }

    suspend fun allJobs(): List<JobRecord> {
        return jobDao.getAll().map { it.asRecord() }
    }

    suspend fun save(job: JobRecord): JobRecord {
        val persistedId = jobDao.insert(job.asEntity())
        return job.copy(id = if (persistedId > 0L) persistedId else job.id)
    }

    suspend fun deleteById(id: Long) {
        jobDao.deleteById(id)
    }
}
