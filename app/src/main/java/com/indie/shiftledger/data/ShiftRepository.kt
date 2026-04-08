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

    suspend fun save(job: JobRecord) {
        jobDao.insert(job.asEntity())
    }

    suspend fun deleteById(id: Long) {
        jobDao.deleteById(id)
    }
}
