# TODO — implement upstream's inherited TODOs (REMOVE ME AFTER FINISH)

REMOVE ME AFTER FINISH: delete this file in the commit that closes the last
issue below.

Session 06 (2026-09-20) found 25 `// TODO` comments that upstream left in
`app/` and that detekt's `ForbiddenComment` rejects. Each one is now a
GitHub issue on this fork (label `upstream-todo`) and the comment was
rewritten as `// Follow-up: <text> (<issue url>)`. This file asks a future
Claude session to actually implement them.

## For the session that picks this up

- Work one issue per commit, `Closes #<n>` in the message; keep the 250-line
  cap, 100 % coverage and the full lint stack green (`scripts/ci_gates.sh`).
- When an issue turns out to be upstream's call rather than this fork's
  (a Google issuetracker workaround, a "when main activity is rewritten in
  Compose"), close it with a one-line reason and delete the `Follow-up`
  comment; do not leave the comment without an open issue behind it.
- The `Follow-up` comment goes when the issue closes, whichever way.

## Issues

- [#4](https://github.com/kuhyx/TachiyomiSY/issues/4) Chapter model: remove the compatibility shim once all deps are migrated
  - `app/src/main/java/eu/kanade/domain/chapter/model/Chapter.kt:8`
- [#5](https://github.com/kuhyx/TachiyomiSY/issues/5) Manga model: move the extension helpers into the domain model
  - `app/src/main/java/eu/kanade/domain/manga/model/Manga.kt:20`
- [#6](https://github.com/kuhyx/TachiyomiSY/issues/6) Trackers: move tracker updates to an interactor over common data
  - `app/src/main/java/eu/kanade/domain/track/interactor/AddTracks.kt:30`
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/Tracker.kt:75`
- [#7](https://github.com/kuhyx/TachiyomiSY/issues/7) AddTracks: merge into SyncChapterProgressWithTrack
  - `app/src/main/java/eu/kanade/domain/track/interactor/AddTracks.kt:41`
- [#8](https://github.com/kuhyx/TachiyomiSY/issues/8) GlobalSearchToolbar: improve the UX of the search filter
  - `app/src/main/java/eu/kanade/presentation/browse/components/GlobalSearchToolbar.kt:76`
- [#9](https://github.com/kuhyx/TachiyomiSY/issues/9) Compose dialogs: drop the workaround for issuetracker 204502668
  - `app/src/main/java/eu/kanade/presentation/category/components/CategoryDialogs.kt:112`
  - `app/src/main/java/eu/kanade/presentation/category/components/CategoryDialogs.kt:176`
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/browse/components/ExtensionStoresDialogs.kt:97`
- [#10](https://github.com/kuhyx/TachiyomiSY/issues/10) LibrarySettingsDialog: re-enable custom update intervals in stable
  - `app/src/main/java/eu/kanade/presentation/library/LibrarySettingsDialog.kt:139`
- [#11](https://github.com/kuhyx/TachiyomiSY/issues/11) LibraryTabs: use the default tab width once fixed upstream
  - `app/src/main/java/eu/kanade/presentation/library/components/LibraryTabs.kt:29`
- [#12](https://github.com/kuhyx/TachiyomiSY/issues/12) MangaInfoHeader: show something better for a custom interval
  - `app/src/main/java/eu/kanade/presentation/manga/components/MangaInfoHeader.kt:192`
- [#13](https://github.com/kuhyx/TachiyomiSY/issues/13) SettingsAdvancedScreen: allow the private option in stable once URL handling is fleshed out
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsAdvancedScreen.kt:491`
- [#14](https://github.com/kuhyx/TachiyomiSY/issues/14) TrackInfoDialogHome: wire the start-date click to a real editor
  - `app/src/main/java/eu/kanade/presentation/track/TrackInfoDialogHome.kt:115`
- [#15](https://github.com/kuhyx/TachiyomiSY/issues/15) BackupRestorer: optionally trigger an online library + tracker update
  - `app/src/main/java/eu/kanade/tachiyomi/data/backup/restore/BackupRestorer.kt:151`
- [#16](https://github.com/kuhyx/TachiyomiSY/issues/16) LibraryUpdateJob: surface skipped reasons to the user
  - `app/src/main/java/eu/kanade/tachiyomi/data/library/LibraryUpdateJob.kt:314`
- [#17](https://github.com/kuhyx/TachiyomiSY/issues/17) Trackers: store all scores on a 10-point scale
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/BaseTracker.kt:44`
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/Tracker.kt:41`
- [#18](https://github.com/kuhyx/TachiyomiSY/issues/18) MyAnimeListInterceptor: add back the custom user agent
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/myanimelist/MyAnimeListInterceptor.kt:35`
- [#19](https://github.com/kuhyx/TachiyomiSY/issues/19) SuwayomiApi: filter on the chapter number
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/suwayomi/SuwayomiApi.kt:88`
- [#20](https://github.com/kuhyx/TachiyomiSY/issues/20) EHentai: consider gallery updating when doing tabbed browsing
  - `app/src/main/java/eu/kanade/tachiyomi/source/online/all/EHentai.kt:98`
- [#21](https://github.com/kuhyx/TachiyomiSY/issues/21) MigrateSourceTab: clean up the sorting code
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/migration/sources/MigrateSourceTab.kt:56`
- [#22](https://github.com/kuhyx/TachiyomiSY/issues/22) MangaCoverScreenModel: handle animated covers
  - `app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaCoverScreenModel.kt:102`
- [#23](https://github.com/kuhyx/TachiyomiSY/issues/23) DisplayExtensions: move the logic to isTabletUi() once MainActivity is Compose
  - `app/src/main/java/eu/kanade/tachiyomi/util/system/DisplayExtensions.kt:25`
- [#24](https://github.com/kuhyx/TachiyomiSY/issues/24) FavoritesSyncHelper: only apply database changes after the sync
  - `app/src/main/java/exh/favorites/FavoritesSyncHelper.kt:51`
