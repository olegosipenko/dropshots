package com.dropbox.dropshots

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.dropbox.differ.SimpleImageComparator
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DropshotsTest {

  private val fakeValidator = FakeResultValidator()

  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

  @get:Rule
  val dropshots = Dropshots(
    recordScreenshots = false,
    resultValidator = fakeValidator,
    imageComparator = SimpleImageComparator(
      maxDistance = 0.004f,
      hShift = 1,
      vShift = 1,
    )
  )

  @Before
  fun setup() {
    fakeValidator.validator = CountValidator(0)
    activityScenarioRule.scenario.onActivity { activity ->
      activity.setContentView(
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL

          listOf(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN).forEach {
            addView(
              View(activity).apply {
                setBackgroundColor(it)
                layoutParams = LinearLayout.LayoutParams(
                  ViewGroup.MarginLayoutParams.MATCH_PARENT,
                  ViewGroup.MarginLayoutParams.WRAP_CONTENT,
                  1f
                )
              }
            )
          }
        }
      )
    }
  }

  @Test
  fun testMatchesActivityScreenshot() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(it, "MatchesActivityScreenshot")
    }
  }

  @Test
  fun testMatchesViewScreenshot() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(
        it.findViewById<View>(android.R.id.content),
        name = "MatchesViewScreenshot"
      )
    }
  }

  @Test
  fun testFailsForDifferences() {
    var failed = false
    activityScenarioRule.scenario.onActivity {
      try {
        Log.d("!!! TEST !!!", "Asserting snapshot...")
        dropshots.assertSnapshot(
          view = it.findViewById(android.R.id.content),
          name = "MatchesViewScreenshotBad"
        )
        Log.d("!!! TEST !!!", "Snapshot asserted")
        failed = true
      } catch (e: AssertionError) {
        Log.d("!!! TEST !!!", "Snapshot assertion failed as expected.")
        // pass
      }
    }

    Log.d("!!! TEST !!!", "Validating thrown error")
    if (failed) {
      fail("Expected error when screenshots differ.")
    }
  }

  @Test
  fun testPassesWhenValidatorPasses() {
    fakeValidator.validator = { true }
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(
        view = it.findViewById(android.R.id.content),
        name = "MatchesViewScreenshotBad"
      )
    }
  }

  @Test
  fun testFailsWhenValidatorFails() {
    fakeValidator.validator = { false }

    var caughtError: AssertionError? = null
    activityScenarioRule.scenario.onActivity {
      try {
        dropshots.assertSnapshot(
          view = it.findViewById(android.R.id.content),
          name = "MatchesViewScreenshotBad"
        )
      } catch (e: AssertionError) {
        caughtError = e
      }
    }

    assertNotNull(caughtError)
  }

  @Test
  fun fastFailsForMismatchedSize() {
    activityScenarioRule.scenario.onActivity {
      try {
        dropshots.assertSnapshot(
          view = it.findViewById(android.R.id.content),
          name = "MatchesViewScreenshotBadSize"
        )
        fail("Expected error when screenshots differ.")
      } catch (e: Throwable) {
        assertTrue(e.message!!.contains("Output written to: "))
        val path = e.message!!.lines()[1].removePrefix("Output written to: ")
        val outputFile = File(path)
        assertTrue("File expected to exist at: $path", outputFile.exists())

        val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
        assertTrue("Output image expected to be twice the captured width.", bitmap.width == 1080 * 2)
      }
    }
  }
}

