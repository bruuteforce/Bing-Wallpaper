package com.example.bingdailywallpapers

import com.example.bingdailywallpapers.data.model.BingImage
import org.junit.Assert.assertEquals
import org.junit.Test

class BingImageTest {

    @Test
    fun testPortraitUrlConversion_standard1920x1080() {
        val image = BingImage(
            startdate = "20260330",
            fullstartdate = "202603300000",
            enddate = "20260331",
            url = "/th?id=OHR.Test_EN-IN1234567890_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp",
            urlbase = "/th?id=OHR.Test_EN-IN1234567890",
            copyright = "Copyright",
            copyrightlink = "",
            title = "Test",
            quiz = "",
            wp = true,
            hsh = "123"
        )
        val portrait = image.portraitUrl
        assertEquals(
            "https://www.bing.com/th?id=OHR.Test_EN-IN1234567890_1080x1920.jpg&rf=LaDigue_1080x1920.jpg&pid=hp",
            portrait
        )
    }

    @Test
    fun testPortraitUrlConversion_uhd() {
        val image = BingImage(
            startdate = "20260330",
            fullstartdate = "202603300000",
            enddate = "20260331",
            url = "/th?id=OHR.Test_EN-IN1234567890_UHD.jpg&rf=LaDigue_UHD.jpg&pid=hp",
            urlbase = "/th?id=OHR.Test_EN-IN1234567890",
            copyright = "Copyright",
            copyrightlink = "",
            title = "Test",
            quiz = "",
            wp = true,
            hsh = "123"
        )
        val portrait = image.portraitUrl
        assertEquals(
            "https://www.bing.com/th?id=OHR.Test_EN-IN1234567890_1080x1920.jpg&rf=LaDigue_1080x1920.jpg&pid=hp",
            portrait
        )
    }

    @Test
    fun testPortraitUrlConversion_fallbackIfNotMatched() {
        val image = BingImage(
            startdate = "20260330",
            fullstartdate = "202603300000",
            enddate = "20260331",
            url = "https://images.unsplash.com/photo-custom",
            urlbase = "",
            copyright = "Copyright",
            copyrightlink = "",
            title = "Test",
            quiz = "",
            wp = true,
            hsh = "123"
        )
        val portrait = image.portraitUrl
        assertEquals("https://images.unsplash.com/photo-custom", portrait)
    }
}
