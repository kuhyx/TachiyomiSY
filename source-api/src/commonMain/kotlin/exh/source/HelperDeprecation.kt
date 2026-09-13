package exh.source

/** Message of the helper overrides a delegating source must never reach. */
internal const val NEVER_CALLED: String = "Should never be called!"

/** The deprecation message HttpSource puts on its request/parse helper pairs. */
internal const val HELPER_DEPRECATION: String =
    "The helper functions are inherently limiting and hides the underlying implementation. " +
        "Source developers should make their own implementation according to their needs."
