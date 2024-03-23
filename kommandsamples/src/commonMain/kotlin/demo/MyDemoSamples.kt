package pl.mareklangiewicz.kommand.demo

import pl.mareklangiewicz.annotations.DelicateApi
import pl.mareklangiewicz.annotations.ExampleApi
import pl.mareklangiewicz.bad.bad
import pl.mareklangiewicz.kommand.CLI.Companion.SYS
import pl.mareklangiewicz.kommand.InteractiveScript
import pl.mareklangiewicz.kommand.Kommand
import pl.mareklangiewicz.kommand.ReducedKommand
import pl.mareklangiewicz.kommand.ReducedScript
import pl.mareklangiewicz.kommand.ZenityOpt.*
import pl.mareklangiewicz.kommand.admin.btop
import pl.mareklangiewicz.kommand.bash
import pl.mareklangiewicz.kommand.bashGetExportsToFile
import pl.mareklangiewicz.kommand.core.cat
import pl.mareklangiewicz.kommand.ax
import pl.mareklangiewicz.kommand.ideOpen
import pl.mareklangiewicz.kommand.kommand
import pl.mareklangiewicz.kommand.konfig.getKeyValStr
import pl.mareklangiewicz.kommand.konfig.konfigInDir
import pl.mareklangiewicz.kommand.konfig.konfigInUserHomeConfigDir
import pl.mareklangiewicz.kommand.konfig.logEachKeyVal
import pl.mareklangiewicz.kommand.man
import pl.mareklangiewicz.kommand.samples.s
import pl.mareklangiewicz.kommand.setUserFlag
import pl.mareklangiewicz.kommand.term.termKitty
import pl.mareklangiewicz.kommand.zenity
import pl.mareklangiewicz.kommand.zenityAskForEntry
import pl.mareklangiewicz.kommand.zenityAskIf

/**
 * A bunch of samples to show on my machine when presenting KommandLine.
 * So it might be not the best idea to actually ax all these kommands on other machines.
 * (SamplesTests just check generated kommand lines, without executing any kommands)
 */
@ExampleApi
@OptIn(DelicateApi::class)
data object MyDemoSamples {

    val btop = btop() s
            "btop"

    val btopK = termKitty(btop) s
            "kitty -1 --detach -- btop"

    val man1 = InteractiveScript {
        val page = getEntry("manual page for")
        termKitty(man { +page }).x()
    }

    val ps1 = termKitty(bash("ps -e | grep java"), hold = true) s
            "kitty -1 --detach --hold -- bash -c ps -e | grep java"

    val ps2 = InteractiveScript {
        val process = getEntry("find process")
        termKitty(bash("ps -e | grep $process"), hold = true).x()
    }

    val catFstabAndHosts = cat { +"/etc/fstab"; +"/etc/hosts" } s
            "cat /etc/fstab /etc/hosts"

    val catFstabAndHostsK = termKitty(catFstabAndHosts, hold = true) s
            "kitty -1 --detach --hold -- cat /etc/fstab /etc/hosts"

    val ideOpen1 = InteractiveScript {
        val path = getEntry("open file in IDE", suggested = "/home/marek/.bashrc")
        ideOpen(path).x()
    }

    val ideOpenBashExports = InteractiveScript {
        bashGetExportsToFile(tmpNotesFile).x()
        ideOpen(tmpNotesFile).x()
    }

    val ideOpenXClip = InteractiveScript {
        kommand("xclip", "-o").ax(it, outFile = tmpNotesFile)
        // bash("xclip -o > $tmpNotesFile").x() // equivalent to above
        ideOpen(tmpNotesFile).x()
    }

    // Note: not InteractiveScript because I want to be able to enable interactive code when it's disabled.
    val iCodeSwitch = ReducedScript {
        val enabled = askIf("Should interactive code be enabled?")
        setUserFlag(SYS, "code.interactive", enabled)
        showInfo("user flag: code.interactive.enabled = $enabled")
    }

    val myDemoTestsSwitch = InteractiveScript {
        val enabled = askIf("Should MyDemoTests be enabled?")
        setUserFlag(SYS, "tests.MyDemoTests", enabled)
        showInfo("user flag: tests.MyDemoTests.enabled = $enabled")
    }

    val showWholeUserConfig = InteractiveScript {
        val konfig = konfigInUserHomeConfigDir(SYS)
        showInfo(konfig.keys.map { konfig.getKeyValStr(it) }.joinToString("\n\n"))
    }

    val playWithKonfigExamples = InteractiveScript {
        val k = konfigInDir("/home/marek/tmp/konfig_examples", checkForDangerousValues = false)
        println("before adding anything:")
        k.logEachKeyVal()
        k["tmpExampleInteger1"] = 111.toString()
        k["tmpExampleInteger2"] = 222.toString()
        k["tmpExampleString1"] = "some text 1"
        k["tmpExampleString2"] = "some text 2"
        println("after adding 4 keys:")
        k.logEachKeyVal()
        k["tmpExampleInteger2"] = null
        k["tmpExampleString2"] = null
        println("after nulling 2 keys:")
        k.logEachKeyVal()
        k["tmpExampleInteger1"] = null
        k["tmpExampleString1"] = null
        println("after nulling other 2 keys:")
        k.logEachKeyVal()
    }
}



private suspend fun Kommand.x() = ax(SYS)
private suspend fun <T> ReducedKommand<T>.x() = ax(SYS)
private suspend fun <T> ReducedScript<T>.x() = ax(SYS)

@OptIn(DelicateApi::class)
private suspend fun showInfo(info: String) = zenity(Type.Info) { -Text(info) }.x()

@OptIn(DelicateApi::class)
private suspend fun showError(error: String) = zenity(Type.Error) { -Text(error) }.x()

private suspend fun askIf(question: String) = zenityAskIf(question).x()

private suspend fun askEntry(question: String, suggested: String? = null) =
    zenityAskForEntry(question, suggested = suggested).x()?.takeIf { it.isNotBlank() }

private suspend fun getEntry(question: String, suggested: String? = null, errorMsg: String = "User didn't answer.") =
    askEntry(question, suggested) ?: run { showError(errorMsg); bad { errorMsg } }

private val tmpNotesFile = SYS.pathToUserTmp + "/tmp.notes"

