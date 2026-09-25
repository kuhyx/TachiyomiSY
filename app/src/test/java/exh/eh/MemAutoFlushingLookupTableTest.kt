package exh.eh

import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.ByteBuffer

private const val HEADER = 8

@RunWith(RobolectricTestRunner::class)
internal class MemAutoFlushingLookupTableTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val strings = object : MemAutoFlushingLookupTable.EntrySerializer<String> {
        override fun write(entry: String) = entry
        override fun read(string: String) = string
    }

    @Before
    fun initLogging() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
    }

    private fun file() = File(folder.root, "table.maftable")

    private fun waitFor(what: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!what() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        what().shouldBeTrue()
    }

    private fun <T> withTable(debounce: Long = 10, block: MemAutoFlushingLookupTable<String>.() -> T): T {
        val table = MemAutoFlushingLookupTable(file(), strings, debounce)
        try {
            return table.block()
        } finally {
            // close() cancels the runBlocking it runs in rather than the table's scope, so it always throws.
            runCatching { table.close() }
        }
    }

    private fun runHook(table: MemAutoFlushingLookupTable<String>) {
        val field = MemAutoFlushingLookupTable::class.java.getDeclaredField("shutdownHook")
        field.isAccessible = true
        (field.get(table) as Thread).run()
    }

    @Test
    fun putsPersistAndReload() {
        val big = "x".repeat(20_000)
        withTable {
            runBlocking {
                put(1, "one")
                put(2, big)
                size() shouldBe 2
                get(1) shouldBe "one"
                get(3).shouldBeNull()
            }
            waitFor { file().length() > big.length }
        }
        withTable {
            runBlocking {
                get(1) shouldBe "one"
                get(2) shouldBe big
                size() shouldBe 2
            }
        }
    }

    @Test
    fun defaultDebounceIsThreeSeconds() {
        val table = MemAutoFlushingLookupTable(file(), strings)
        runBlocking { table.put(5, "five") }
        Thread.sleep(200)
        file().exists() shouldBe false
        runHook(table)
        file().length() shouldBe HEADER + 4
        runCatching { table.close() }
    }

    @Test
    fun truncatedEntriesStopTheLoad() {
        val payload = "hello".encodeToByteArray()
        val bytes = ByteBuffer.allocate(HEADER * 2 + payload.size + 2)
        bytes.putInt(7).putInt(payload.size).put(payload)
        bytes.putInt(8).putInt(100).put(byteArrayOf(1, 2))
        file().writeBytes(bytes.array())
        withTable {
            runBlocking {
                get(7) shouldBe "hello"
                get(8).shouldBeNull()
                size() shouldBe 1
            }
        }
    }

    @Test
    fun shutdownHookSkipsWhenFlushed() {
        withTable {
            runHook(this)
            file().exists() shouldBe false
        }
    }

    @Test
    fun failedWritesAreAbandoned() {
        val failing = object : MemAutoFlushingLookupTable.EntrySerializer<String> {
            override fun write(entry: String): String = error("cannot serialise $entry")
            override fun read(string: String) = string
        }
        val table = MemAutoFlushingLookupTable(file(), failing, 100_000)
        runBlocking { table.put(1, "one") }
        shouldThrow<IllegalStateException> { runHook(table) }
        file().exists() shouldBe false
        runCatching { table.close() }
    }

    @Test
    fun rapidPutsCoalesce() {
        withTable(debounce = 0) {
            runBlocking { repeat(300) { put(it % 3, "v$it") } }
            waitFor { file().exists() && runBlocking { size() } == 3 }
            Thread.sleep(100)
        }
        withTable {
            runBlocking { size() } shouldBe 3
        }
    }
}
