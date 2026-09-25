package eu.kanade.tachiyomi.data.backup

import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.Futures.immediateFuture
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

/** Parameters for a worker run with [input], whose foreground requests all succeed. */
internal fun workerParams(input: Data = Data.EMPTY, tags: Set<String> = emptySet()): WorkerParameters {
    val updater = mockk<ForegroundUpdater> {
        every { setForegroundAsync(any(), any(), any()) } returns immediateFuture(null)
    }
    val params = mockk<WorkerParameters>(relaxed = true)
    every { params.inputData } returns input
    every { params.tags } returns tags
    every { params.id } returns UUID.randomUUID()
    every { params.foregroundUpdater } returns updater
    return params
}
