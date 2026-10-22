package Sakuin.Project.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {
    @Test
    fun subsamplesWhileStayingAboveTheTarget() {
        assertEquals(1, sampleSizeFor(1200, 1600, 1600))
        assertEquals(1, sampleSizeFor(3000, 2000, 1600))
        assertEquals(2, sampleSizeFor(4000, 3000, 1600))
        assertEquals(4, sampleSizeFor(3000, 8000, 1600))
    }

    @Test
    fun scalesTheLongSideDown() {
        assertEquals(1200 to 1600, scaledSize(1200, 1600, 1600))
        assertEquals(1600 to 1200, scaledSize(4000, 3000, 1600))
        assertEquals(600 to 1600, scaledSize(1500, 4000, 1600))
        assertEquals(1 to 1600, scaledSize(1, 5000, 1600))
    }
}
