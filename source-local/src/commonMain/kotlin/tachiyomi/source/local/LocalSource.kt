package tachiyomi.source.local

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.UnmeteredSource

/** The built-in source that reads manga from a directory on the device. */
public expect class LocalSource : Source, UnmeteredSource
