package nl.potentplum.mise

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareIntentsTest {
    private fun send(
        type: String,
        build: Intent.() -> Unit,
    ) = Intent(Intent.ACTION_SEND).setType(type).apply(build)

    @Test
    fun `chrome shares a title and a link`() {
        val intent = send("text/plain") { putExtra(Intent.EXTRA_TEXT, "Pompoensoep\nhttps://www.ah.nl/allerhande/recept/R-1") }
        assertEquals(SharedContent.Link("https://www.ah.nl/allerhande/recept/R-1"), ShareIntents.read(intent))
    }

    @Test
    fun `text without a link is ignored`() {
        assertNull(ShareIntents.read(send("text/plain") { putExtra(Intent.EXTRA_TEXT, "Lekker!") }))
    }

    @Test
    fun `one or more images`() {
        val photo = Uri.parse("content://photos/1")
        assertEquals(SharedContent.Images(listOf(photo)), ShareIntents.read(send("image/jpeg") { putExtra(Intent.EXTRA_STREAM, photo) }))
        val many =
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/*")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(photo, Uri.parse("content://photos/2")))
        assertEquals(2, (ShareIntents.read(many) as SharedContent.Images).uris.size)
    }

    @Test
    fun `other intents are ignored`() {
        assertNull(ShareIntents.read(Intent(Intent.ACTION_VIEW)))
        assertNull(ShareIntents.read(null))
    }
}
