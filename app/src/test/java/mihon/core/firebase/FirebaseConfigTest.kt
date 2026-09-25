package mihon.core.firebase

import android.content.Context
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class FirebaseConfigTest {
    @Test
    fun everyHookIsANoOp() {
        FirebaseConfig.init(mockk<Context>())
        FirebaseConfig.setAnalyticsEnabled(true)
        FirebaseConfig.setAnalyticsEnabled(false)
        FirebaseConfig.setCrashlyticsEnabled(true)
        FirebaseConfig.setCrashlyticsEnabled(false)
    }
}
