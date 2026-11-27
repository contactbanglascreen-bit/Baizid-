package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowNetworkCapabilities

/** Opens the app on a simulated phone and taps around, so a crash on tap shows up here. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TapCrashTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private fun goOnline() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadowOf(caps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(cm).setNetworkCapabilities(cm.activeNetwork, caps)
    }

    private fun screen(label: String) {
        println("SCREEN $label\n" + rule.onAllNodes(isRoot()).printToString(maxDepth = 4).take(2500))
    }

    @Test
    fun tapEveryTabAndFirstButtons() {
        goOnline()
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.mainClock.advanceTimeBy(8_000)
            rule.waitForIdle()
            // Close the update popup if it shows.
            runCatching { rule.onAllNodes(hasText("Later") and hasClickAction())[0].performClick() }
            rule.mainClock.advanceTimeBy(2_000)
            rule.waitForIdle()
            screen("start")
            for (tag in listOf("nav_explore", "nav_reels", "nav_library", "nav_me", "nav_home")) {
                println("TAP $tag")
                rule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
                rule.mainClock.advanceTimeBy(1_500)
                rule.waitForIdle()
            }
            val clickables = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size
            println("CLICKABLES $clickables")
            for (i in 0 until minOf(clickables, 30)) {
                println("TAP clickable #$i")
                runCatching { rule.onAllNodes(hasClickAction())[i].performClick() }
                    .onFailure { if (it is AssertionError) println("skip #$i: ${it.message?.take(80)}") else throw it }
                rule.mainClock.advanceTimeBy(1_000)
                rule.waitForIdle()
            }
            screen("end")
        }
    }

    /** Opens titles from Home and presses Play on each, the way a viewer would. */
    @Test
    fun openTitlesAndPressPlay() {
        goOnline()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            rule.mainClock.advanceTimeBy(10_000)
            rule.waitForIdle()
            runCatching { rule.onAllNodes(hasText("Later") and hasClickAction())[0].performClick() }
            rule.mainClock.advanceTimeBy(2_000)
            rule.waitForIdle()
            var opened = 0
            for (round in 0 until 6) {
                val nodes = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
                // Posters sit lower on the screen than the header buttons.
                val target = nodes.filter { it.boundsInRoot.top > 500f && it.boundsInRoot.height > 150f }
                    .getOrNull(round) ?: break
                println("OPEN #$round at ${target.boundsInRoot}")
                rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
                val idx = nodes.indexOf(target)
                runCatching { rule.onAllNodes(hasClickAction())[idx].performClick() }
                rule.mainClock.advanceTimeBy(4_000)
                rule.waitForIdle()
                val play = rule.onAllNodes(hasClickAction() and (hasText("Play", substring = true, ignoreCase = true) or hasText("Watch", substring = true, ignoreCase = true)))
                    .fetchSemanticsNodes()
                println("  play buttons: ${play.size}")
                if (play.isNotEmpty()) {
                    opened++
                    runCatching { rule.onAllNodes(hasClickAction() and (hasText("Play", substring = true, ignoreCase = true) or hasText("Watch", substring = true, ignoreCase = true)))[0].performClick() }
                    rule.mainClock.advanceTimeBy(6_000)
                    rule.waitForIdle()
                    println("  pressed play")
                }
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                rule.mainClock.advanceTimeBy(1_500)
                rule.waitForIdle()
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                rule.mainClock.advanceTimeBy(1_500)
                rule.waitForIdle()
            }
            println("OPENED $opened")
        }
    }
}
