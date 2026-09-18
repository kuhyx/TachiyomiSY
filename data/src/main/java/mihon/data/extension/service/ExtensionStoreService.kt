package mihon.data.extension.service

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.data.extension.model.BaseNetworkExtensionStore
import mihon.data.extension.model.NetworkExtensionStore
import mihon.data.extension.model.NetworkLegacyExtension
import mihon.data.extension.model.NetworkLegacyExtensionRepo
import mihon.data.extension.model.toAvailableExtensions
import mihon.domain.extension.model.ExtensionStore
import okio.BufferedSource
import okio.buffer
import okio.gzip
import tachiyomi.core.common.util.system.logcat
import kotlin.coroutines.cancellation.CancellationException

/**
 * Fetches extension stores and their extension lists over the network. Three
 * formats are understood by sniffing the first byte: a protobuf store, a JSON
 * store or legacy repo (`{`), and a legacy extension array (`[`) whose store
 * is then read from the sibling `repo.json`.
 */
public class ExtensionStoreService(
    private val network: NetworkHelper,
    private val json: Json,
    private val protoBuf: ProtoBuf,
) {
    /** The store at [indexUrl], following a legacy repo's `indexV2` pointer; a failure is the [Result]. */
    public suspend fun fetch(indexUrl: String): Result<ExtensionStore> {
        return try {
            val (networkStore, resolvedUrl) = decodeStore(indexUrl)
            if (networkStore is NetworkLegacyExtensionRepo && networkStore.indexV2 != null) {
                fetch(networkStore.indexV2)
            } else {
                Result.success(networkStore.toExtensionStore(resolvedUrl))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (expected: Exception) {
            // Any network or parse failure is logged and reported as the Result.
            logcat(LogPriority.ERROR, expected) { "Failed to add extension store '$indexUrl'" }
            Result.failure(expected)
        }
    }

    // The decoded store and the url it was really read from (legacy arrays redirect to repo.json).
    private suspend fun decodeStore(indexUrl: String): Pair<BaseNetworkExtensionStore, String> {
        val response = network.client.newCall(GET(indexUrl)).awaitSuccess()
        return response.body.source().decompressIfGzipped().use { source ->
            when (source.peek().readByte()) {
                JSON_ARRAY_START -> decodeLegacyRepo(indexUrl)
                JSON_OBJECT_START -> decodeJsonStore(source) to indexUrl
                else -> protoBuf.decodeFromByteArray<NetworkExtensionStore>(source.readByteArray()) to indexUrl
            }
        }
    }

    private suspend fun decodeLegacyRepo(indexUrl: String): Pair<BaseNetworkExtensionStore, String> {
        require(indexUrl.endsWith(LEGACY_INDEX)) { "Provided legacy store url is not valid" }
        val repoUrl = indexUrl.replace(LEGACY_INDEX, LEGACY_REPO)
        val repo = network.client.newCall(GET(repoUrl)).awaitSuccess().body.source().use {
            json.decodeFromBufferedSource<NetworkLegacyExtensionRepo>(it)
        }
        return repo to repoUrl
    }

    private fun decodeJsonStore(source: BufferedSource): BaseNetworkExtensionStore {
        return try {
            json.decodeFromBufferedSource<NetworkLegacyExtensionRepo>(source.peek())
        } catch (_: IllegalArgumentException) {
            json.decodeFromBufferedSource<NetworkExtensionStore>(source)
        }
    }

    /** The extensions [store] offers; a failure is the [Result]. */
    public suspend fun getExtensions(store: ExtensionStore): Result<List<Extension.Available>> {
        return try {
            val listUrl = store.extensionListUrl
            val extensions = when {
                listUrl != null -> decodeExtensionList(listUrl).toAvailableExtensions(store)
                !store.isLegacy -> decodeStoreExtensions(store.indexUrl).toAvailableExtensions(store)
                else -> fetchLegacyExtensions(store)
            }
            Result.success(extensions)
        } catch (e: CancellationException) {
            throw e
        } catch (expected: Exception) {
            // Any network or parse failure of a store is reported as the Result.
            Result.failure(expected)
        }
    }

    private suspend fun decodeExtensionList(listUrl: String): NetworkExtensionStore.ExtensionList {
        val response = network.client.newCall(GET(listUrl)).awaitSuccess()
        return response.body.source().decompressIfGzipped().use { source ->
            if (source.peek().readByte() == JSON_OBJECT_START) {
                json.decodeFromBufferedSource<NetworkExtensionStore.ExtensionList>(source)
            } else {
                protoBuf.decodeFromByteArray<NetworkExtensionStore.ExtensionList>(source.readByteArray())
            }
        }
    }

    private suspend fun decodeStoreExtensions(indexUrl: String): NetworkExtensionStore.ExtensionList {
        val response = network.client.newCall(GET(indexUrl)).awaitSuccess()
        return response.body.source().decompressIfGzipped().use { source ->
            if (source.peek().readByte() == JSON_OBJECT_START) {
                json.decodeFromBufferedSource<NetworkExtensionStore>(source)
            } else {
                protoBuf.decodeFromByteArray<NetworkExtensionStore>(source.readByteArray())
            }
        }.extensionList!!
    }

    private suspend fun fetchLegacyExtensions(store: ExtensionStore): List<Extension.Available> {
        val storeBaseUrl = store.indexUrl.removeSuffix(LEGACY_REPO)
        val response = network.client.newCall(GET("$storeBaseUrl$LEGACY_INDEX")).awaitSuccess()
        return response.body.source().use { source ->
            json.decodeFromBufferedSource<List<NetworkLegacyExtension>>(source)
                .map { it.toAvailableExtension(store, storeBaseUrl) }
        }
    }

    private fun BufferedSource.decompressIfGzipped(): BufferedSource {
        val isGzip = peek().use { peeked ->
            try {
                peeked.readShort().toInt() == GZIP_MAGIC
            } catch (_: Exception) {
                false
            }
        }

        return if (isGzip) gzip().buffer() else this
    }

    private companion object {
        const val JSON_ARRAY_START: Byte = '['.code.toByte()
        const val JSON_OBJECT_START: Byte = '{'.code.toByte()
        const val GZIP_MAGIC = 0x1f8b
        const val LEGACY_INDEX = "/index.min.json"
        const val LEGACY_REPO = "/repo.json"
    }
}
