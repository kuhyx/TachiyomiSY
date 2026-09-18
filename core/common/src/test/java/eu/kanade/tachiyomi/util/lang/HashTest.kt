package eu.kanade.tachiyomi.util.lang

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private const val SHA256_EMPTY = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
private const val SHA256_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
private const val MD5_EMPTY = "d41d8cd98f00b204e9800998ecf8427e"
private const val MD5_ABC = "900150983cd24fb0d6963f7d28e17f72"

internal class HashTest {
    @Test
    fun sha256OfBytesAndStrings() {
        Hash.sha256(byteArrayOf()) shouldBe SHA256_EMPTY
        Hash.sha256("abc".toByteArray()) shouldBe SHA256_ABC
        Hash.sha256("abc") shouldBe SHA256_ABC
    }

    @Test
    fun md5OfBytesAndStrings() {
        Hash.md5(byteArrayOf()) shouldBe MD5_EMPTY
        Hash.md5("abc".toByteArray()) shouldBe MD5_ABC
        Hash.md5("abc") shouldBe MD5_ABC
    }
}
