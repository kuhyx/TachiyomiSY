package eu.kanade.tachiyomi.data.track

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import tachiyomi.i18n.MR
import tachiyomi.domain.track.model.Track as DomainTrack

/** The smallest concrete [BaseTracker]: every abstract member answers with a constant or records its input. */
internal class FakeTracker(
    id: Long = 42L,
    private val onUpdate: (Track) -> Track = { it },
) : BaseTracker(id, "Fake") {

    val updated = mutableListOf<Track>()

    override fun getLogo(): Int = 0

    override fun getStatusList(): List<Long> = listOf(READING, COMPLETED, REREADING)

    override fun getStatus(status: Long): StringResource? = if (status == READING) MR.strings.reading else null

    override fun getReadingStatus(): Long = READING

    override fun getRereadingStatus(): Long = REREADING

    override fun getCompletionStatus(): Long = COMPLETED

    override fun getScoreList(): List<String> = listOf("0", "5", "10")

    override fun displayScore(track: DomainTrack): String = track.score.toString()

    override suspend fun update(track: Track, didReadChapter: Boolean): Track {
        updated += track
        return onUpdate(track)
    }

    override suspend fun bind(track: Track, hasReadChapters: Boolean): Track = track

    override suspend fun search(query: String): List<TrackSearch> = emptyList()

    override suspend fun refresh(track: Track): Track = track

    override suspend fun login(username: String, password: String) = saveCredentials(username, password)

    companion object {
        const val READING = 1L
        const val COMPLETED = 2L
        const val REREADING = 3L
    }
}
