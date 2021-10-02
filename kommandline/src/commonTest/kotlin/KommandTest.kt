package pl.mareklangiewicz.kommand

import pl.mareklangiewicz.kommand.Adb.Command.devices
import pl.mareklangiewicz.kommand.Adb.Option
import pl.mareklangiewicz.kommand.Adb.Option.usb
import pl.mareklangiewicz.kommand.Ls.Option.*
import pl.mareklangiewicz.kommand.Ls.Option.sortType.*
import pl.mareklangiewicz.kommand.Vim.Option.gui
import pl.mareklangiewicz.kommand.Vim.Option.servername
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO_someday: intellij plugin with @param UI similar to colab notebooks
//const val USER_ENABLED = true
const val USER_ENABLED = false

fun Kommand.checkWithUser(expectedKommandLine: String, execInDir: String? = null) {
    this.println()
    assertEquals(expectedKommandLine, line())
    if (USER_ENABLED) execInGnomeTermIfUserConfirms(execInDir = execInDir)
}


class KommandTest {
    @Test fun testQuoteShSpecials() {
        val str = "abc|&;<def>(ghi) 1 2  3 \"\\jkl\t\nmno"
        val out = str.quoteBashMetaChars()
        println(str)
        println(out)
        assertEquals("abc\\|\\&\\;\\<def\\>\\(ghi\\)\\ 1\\ 2\\ \\ 3\\ \\\"\\\\jkl\\\t\\\nmno", out)
    }
    @Test fun testLs() = ls { -all; -author; -long; -sort(TIME); +".."; +"/usr" }
        .checkWithUser("ls -a --author -l --sort=time .. /usr")
    @Test fun testAdb() = adb(devices) { -Option.all; -usb }
        .checkWithUser("adb -a -d devices")
    @Test fun testVim() {
        val kommand = vim(".") { -gui; -servername("DDDD") }
        assertEquals(listOf("-g", "--servername", "DDDD", "."), kommand.args)
        kommand.checkWithUser("vim -g --servername DDDD .")
    }
    @Test fun testBash() {
        val kommand1 = vim(".") { -gui; -servername("DDDD") }
        val kommand2 = bash(kommand1)
        assertEquals(listOf("-c", "vim -g --servername DDDD ."), kommand2.args)
        kommand2.checkWithUser("bash -c vim\\ -g\\ --servername\\ DDDD\\ .")
    }
}
