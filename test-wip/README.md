# Unfinished tests from the 2026-09-25 coverage wave

81 test files three agents were part-way through when their run was cut short.
They are **here, outside the `app` module, rather than in `app/src/test/java/`**
because several of them do not compile yet and the test source set has to stay
green. Anything under `app/` is detekt's input whether or not Gradle compiles it,
so a parking spot inside the module needs a rule exclusion; this one needs none --
no Gradle task reads this directory.

Covered here: `eu/kanade/tachiyomi/data/{backup,sync,download,cache,coil,
database,notification,export,preference}`, `eu/kanade/tachiyomi/extension/**`,
`exh/ui/**`, `mihon/{feature,core,test}/**`.

## How to use it

Move the files you need into `app/src/test/java/<same path>` and finish them --
they are the head start, not the deliverable. Known breakage at the time they
were parked:

- `data/download/DownloadProviderFindTest.kt`: `sorted()` on a receiver that is
  not `Iterable<Comparable>`.
- `data/sync/service/*`: they import
  `eu.kanade.tachiyomi.data.track.MapPreferenceStore`, which is `internal` to
  its own file's package -- give the sync tests their own store, or widen it.
- Everything else was unverified: compile it before trusting it.

Delete a file from this directory once its finished version lands in
`app/src/test/java/`, and delete the whole directory when it is empty.
