package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TtsLanguage
import com.example.util.LanguageDetector
import com.example.util.TextChunker
import com.example.util.WavAudioHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KRITY AI TTS", appName)
  }

  @Test
  fun `detect bengali text correctly`() {
    val bengaliText = "নমস্কার! কৃটি এআই টিটিএসে আপনাকে স্বাগতম।"
    val analysis = LanguageDetector.analyze(bengaliText)
    assertEquals(TtsLanguage.BENGALI, analysis.detectedLanguage)
    assertTrue(analysis.charCount > 0)
    assertTrue(analysis.wordCount > 0)
  }

  @Test
  fun `detect hindi text correctly`() {
    val hindiText = "नमस्ते! आज का दिन बहुत सुंदर है।"
    val analysis = LanguageDetector.analyze(hindiText)
    assertEquals(TtsLanguage.HINDI, analysis.detectedLanguage)
    assertTrue(analysis.charCount > 0)
  }

  @Test
  fun `detect english text correctly`() {
    val englishText = "Hello and welcome to KRITY AI voice studio."
    val analysis = LanguageDetector.analyze(englishText)
    assertEquals(TtsLanguage.ENGLISH, analysis.detectedLanguage)
  }

  @Test
  fun `text chunker splits long text at sentence boundaries`() {
    val longText = (1..10).joinToString(" ") { "This is sentence number $it." }
    val chunks = TextChunker.splitIntoChunks(longText, maxLimit = 60)
    assertTrue(chunks.size > 1)
    chunks.forEach { chunk ->
      assertTrue(chunk.isNotBlank())
    }
  }

  @Test
  fun `wav audio helper creates valid 44 byte RIFF header`() {
    val dummyPcm = ByteArray(100)
    val wavBytes = WavAudioHelper.wrapPcmWithWavHeader(dummyPcm, 24000, 1)
    assertEquals(144, wavBytes.size)
    // Check RIFF signature
    assertEquals('R'.code.toByte(), wavBytes[0])
    assertEquals('I'.code.toByte(), wavBytes[1])
    assertEquals('F'.code.toByte(), wavBytes[2])
    assertEquals('F'.code.toByte(), wavBytes[3])
    // Check WAVE signature
    assertEquals('W'.code.toByte(), wavBytes[8])
    assertEquals('A'.code.toByte(), wavBytes[9])
    assertEquals('V'.code.toByte(), wavBytes[10])
    assertEquals('E'.code.toByte(), wavBytes[11])
  }
}
