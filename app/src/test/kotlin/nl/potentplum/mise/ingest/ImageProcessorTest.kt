package nl.potentplum.mise.ingest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageProcessorTest {
    @Test
    fun `sizes keep the aspect ratio and only shrink`() {
        assertEquals(1600 to 1000, ImageProcessor.targetSize(3200, 2000))
        assertEquals(900 to 1600, ImageProcessor.targetSize(1800, 3200))
        assertEquals(800 to 600, ImageProcessor.targetSize(800, 600))
        assertEquals(2, ImageProcessor.sampleSize(4000, 3000))
        assertEquals(1, ImageProcessor.sampleSize(3000, 2000))
    }

    @Test
    fun `a large png becomes a 1600 px jpeg`() {
        val bitmap = Bitmap.createBitmap(3200, 2000, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(63, 94, 42)) }
        val png = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

        val jpeg = ImageProcessor.toJpeg(png)!!
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        assertEquals(1600, bounds.outWidth)
        assertEquals(1000, bounds.outHeight)
        assertEquals("image/jpeg", bounds.outMimeType)
    }

    @Test
    fun `non-images are refused`() {
        assertNull(ImageProcessor.toJpeg("<html>nope</html>".toByteArray()))
    }
}
