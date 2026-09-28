package eu.kanade.presentation.components

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

internal class DialogButtonTest {
    private val action: () -> Unit = {}

    @Test
    fun equalityAndCopies() {
        val ok = DialogButton(text = "OK", onClick = action)
        (ok == ok) shouldBe true
        (ok == DialogButton(text = "OK", onClick = action)) shouldBe true
        ok.equals("OK") shouldBe false
        ok.hashCode() shouldBe DialogButton(text = "OK", onClick = action).hashCode()
        (ok == ok.copy(text = "Cancel")) shouldBe false
        (ok == ok.copy(onClick = {})) shouldBe false
        ok.toString() shouldContain "OK"
        val (text, onClick) = ok
        text shouldBe "OK"
        onClick shouldBe action
    }
}
