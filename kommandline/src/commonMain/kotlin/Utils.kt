package pl.mareklangiewicz.kommand

import pl.mareklangiewicz.kground.*
import pl.mareklangiewicz.kommand.CliPlatform.Companion.SYS
import pl.mareklangiewicz.kommand.gnome.startInTermIfUserConfirms
import pl.mareklangiewicz.kommand.konfig.konfigInUserHomeConfigDir
import pl.mareklangiewicz.kommand.term.*
import kotlin.contracts.*


// the ".enabled" suffix is important, so it's clear the user explicitly enabled a boolean "flag"
fun CliPlatform.isUserFlagEnabled(key: String) = konfigInUserHomeConfigDir()["$key.enabled"]?.trim().toBoolean()
fun CliPlatform.setUserFlag(key: String, enabled: Boolean) { konfigInUserHomeConfigDir()["$key.enabled"] = enabled.toString() }

private val interactive by lazy {
    when {
        SYS.isJvm -> SYS.isUserFlagEnabled("code.interactive")
        else -> {
            println("Interactive stuff is only available on jvm platform (for now).")
            false
        }
    }
}

fun ifInteractive(block: () -> Unit) = if (interactive) block() else println("Interactive code is disabled.")

// FIXME_maybe: stuff like this is a bit too opinionated for kommandline module.
// Maybe move to kommandsamples or somewhere else??
@OptIn(DelicateKommandApi::class)
fun Kommand.chkWithUser(expectedKommandLine: String? = null, execInDir: String? = null, platform: CliPlatform = SYS) {
    this.logln()
    if (expectedKommandLine != null) line().chkEq(expectedKommandLine)
    ifInteractive { platform.startInTermIfUserConfirms(
        kommand = this,
        execInDir = execInDir,
        termKommand = { termKitty(it) },
    ) }
}

class BadExitStateErr(exp: Int, act: Int, message: String? = null): BadEqStateErr(exp, act, message)

inline fun Int.chkExit(exp: Int = 0, lazyMessage: () -> String = { "bad exit $this != $exp" }) {
    this == exp || throw BadExitStateErr(exp, this, lazyMessage())
}

@OptIn(DelicateKommandApi::class)
fun Kommand.chkInIdeap(
    expectedKommandLine: String? = null,
    execInDir: String? = null,
    platform: CliPlatform = SYS
) {
    this.logln()
    if (expectedKommandLine != null) line().chkEq(expectedKommandLine)
    ifInteractive { platform.run {
        val tmpFile = "$pathToUserTmp/tmp.notes"
        start(this@chkInIdeap, dir = execInDir, outFile = tmpFile).waitForExit()
        start(ideap { +tmpFile }).waitForExit()
    } }
}

