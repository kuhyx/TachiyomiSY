package tachiyomi.core.common.util.lang

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.SupervisorJob

/**
 * The application-lifetime scope behind [launchUI], [launchIO] and [launchNow]: never cancelled,
 * with one supervisor so a failed child does not take its siblings down. Prefer a scope tied to
 * the caller's lifecycle whenever one exists.
 */
@DelicateCoroutinesApi
public object AppScope : CoroutineScope by CoroutineScope(SupervisorJob())
