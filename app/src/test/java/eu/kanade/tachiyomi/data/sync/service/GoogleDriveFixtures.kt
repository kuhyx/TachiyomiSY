package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import eu.kanade.tachiyomi.data.backup.models.Backup
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Installed-app OAuth secrets in the shape `client_secrets.json` has. */
internal const val CLIENT_SECRETS = """{"installed":{"client_id":"cid","client_secret":"secret",""" +
    """"redirect_uris":["eu.kanade.google.oauth:/oauth2redirect"],""" +
    """"auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token"}}"""

/**
 * A context with the app's real resources whose assets hold only `client_secrets.json`; the n-th
 * open (1-based) listed in [failOn] throws the paired exception instead.
 */
internal class SecretsContext(
    base: Context,
    private val failOn: Map<Int, IOException> = emptyMap(),
) : ContextWrapper(base) {
    private var opens = 0
    private val assets = mockk<AssetManager> {
        every { open("client_secrets.json") } answers {
            opens++
            failOn[opens]?.let { throw it }
            ByteArrayInputStream(CLIENT_SECRETS.toByteArray())
        }
    }

    override fun getAssets(): AssetManager = assets
}

/** A mocked [Drive] whose appData listing, download, upload and delete requests the tests control. */
internal class FakeDrive {
    val drive: Drive = mockk()
    val files: Drive.Files = mockk()
    val list: Drive.Files.List = mockk()
    val get: Drive.Files.Get = mockk()
    val update: Drive.Files.Update = mockk()
    val create: Drive.Files.Create = mockk()
    val delete: Drive.Files.Delete = mockk()
    val uploads = mutableListOf<Pair<File, Backup>>()

    init {
        every { drive.files() } returns files
        every { files.list() } returns list
        every { list.setSpaces("appDataFolder") } returns list
        every { list.setQ(any()) } returns list
        every { list.setFields(any()) } returns list
        every { files.get(any()) } returns get
        every { files.update(any(), any(), any<AbstractInputStreamContent>()) } answers {
            record(secondArg(), thirdArg())
            update
        }
        every { update.execute() } returns File()
        every { files.create(any(), any<AbstractInputStreamContent>()) } answers {
            record(firstArg(), secondArg())
            create
        }
        every { create.setFields("id") } returns create
        every { create.execute() } returns File().setId("created")
        every { files.delete(any()) } returns delete
        every { delete.execute() } returns null
        listing()
    }

    private fun record(metadata: File, content: AbstractInputStreamContent) {
        val bytes = GZIPInputStream(content.inputStream).use { it.readBytes() }
        uploads += metadata to ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
    }

    /** The appData listing: one file per `id to deviceId` pair. */
    fun listing(vararg files: Pair<String, String?>) {
        val found = files.map { (id, device) ->
            File().setId(id).setAppProperties(device?.let { mapOf("deviceId" to it) } ?: emptyMap())
        }
        every { list.execute() } returns FileList().setFiles(found.toMutableList())
    }

    fun download(backup: Backup) {
        val gzipped = ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { it.write(ProtoBuf.encodeToByteArray(Backup.serializer(), backup)) }
        }
        every { get.executeMediaAsInputStream() } answers { ByteArrayInputStream(gzipped.toByteArray()) }
    }
}
