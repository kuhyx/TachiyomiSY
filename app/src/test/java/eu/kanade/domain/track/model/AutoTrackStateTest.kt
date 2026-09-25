package eu.kanade.domain.track.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class AutoTrackStateTest {

    @Test
    fun eachStateHasATitle() {
        AutoTrackState.entries.size shouldBe 3
        AutoTrackState.ALWAYS.titleRes shouldBe MR.strings.lock_always
        AutoTrackState.ASK.titleRes shouldBe MR.strings.default_category_summary
        AutoTrackState.NEVER.titleRes shouldBe MR.strings.lock_never
        AutoTrackState.valueOf("ASK") shouldBe AutoTrackState.ASK
    }
}
