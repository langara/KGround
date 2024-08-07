@file:OptIn(ExperimentalApi::class)

package pl.mareklangiewicz.kommand

import okio.Path
import pl.mareklangiewicz.kground.io.pth
import pl.mareklangiewicz.udata.strf
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import pl.mareklangiewicz.annotations.DelicateApi
import pl.mareklangiewicz.annotations.ExperimentalApi
import pl.mareklangiewicz.annotations.NotPortableApi
import pl.mareklangiewicz.bad.chkEmpty
import pl.mareklangiewicz.bad.chkEq
import pl.mareklangiewicz.bad.chkThis
import pl.mareklangiewicz.bad.chkThrows
import pl.mareklangiewicz.kground.*
import pl.mareklangiewicz.kgroundx.maintenance.ZenitySupervisor
import pl.mareklangiewicz.kommand.core.*
import pl.mareklangiewicz.kommand.shell.bashQuoteMetaChars
import pl.mareklangiewicz.kommand.konfig.IKonfig
import pl.mareklangiewicz.kommand.konfig.konfigInDir
import pl.mareklangiewicz.uctx.uctx
import pl.mareklangiewicz.udata.str
import pl.mareklangiewicz.ulog.hack.UHackySharedFlowLog
import pl.mareklangiewicz.uspek.USpekContext
import pl.mareklangiewicz.uspek.USpekTree
import pl.mareklangiewicz.uspek.failed
import pl.mareklangiewicz.uspek.so
import pl.mareklangiewicz.uspek.suspek
import pl.mareklangiewicz.uspek.ucontext


@OptIn(DelicateApi::class)
class KommandTests {

  init {
    "INIT ${this::class.simpleName}".teePP
  }

  val platform = getCurrentPlatformKind()

  @Test fun t() = runTestUSpekWithWorkarounds {

    "On string with bash meta chars" so {
      val string = "abc|&;<def>(ghi) 1 2  3 \"\\jkl\t\nmno"
      "bash quote meta chars correctly" so {
        val quoted = bashQuoteMetaChars(string)
        quoted chkEq "abc\\|\\&\\;\\<def\\>\\(ghi\\)\\ 1\\ 2\\ \\ 3\\ \\\"\\\\jkl\\\t\\\nmno"
      }
    }

    if (platform == "JVM") "On JVM only" so { // TODO_someday: On Native? On NodeJs?

      "On real file system on tmp dir" so {

        "On mktemp kommand" so {
          var tmpFile = "/tmp/fake".pth
          try {
            tmpFile = mktemp(path = "/tmp".pth, prefix = "tmpFile").ax()
            "name is fine" so { tmpFile.chkThis { strf.startsWith("/tmp/tmpFile") && strf.endsWith(".tmp") } }
            "file is there" so {
              lsRegFiles("/tmp".pth).ax().chkThis { any { "/tmp/$it" == tmpFile.strf } }
            }
          } finally {
            rmFileIfExists(tmpFile).ax()
          }
        }

        // Note: random dirName can't be in test name bc uspek would loop infinitely finding new "branches"
        val dirName = "testDirTmp" + Random.nextLong().absoluteValue
        val tmpDir = "/tmp".pth / dirName
        val tmpDirBla = tmpDir / "bla"
        val tmpDirBlaBle = tmpDirBla / "ble"

        "On mkdir with parents" so {
          try {
            mkdir(tmpDirBlaBle, withParents = true).chkLineRaw("mkdir -p $tmpDirBlaBle").ax()

            "check created dirs with ls" so {
              lsSubDirs("/tmp".pth).chkLineRaw("ls --indicator-style=slash /tmp")
                .ax().chkThis { strf.contains(dirName) }
            }
            "ls tmp dir is not file" so { lsRegFiles("/tmp".pth).ax().chkThis { !strf.contains(dirName) } }

            "On rm empty ble" so {
              rmDirIfEmpty(tmpDirBlaBle).ax()

              "bla does not contain ble" so { lsSubDirs(tmpDirBla).ax().chkEmpty() }
            }

            "On touchy blu file" so {
              val bluName = "blu.touchy"
              val bluPath = tmpDirBlaBle / bluName
              touch(bluPath).ax()

              "ls blu is there" so { lsRegFiles(tmpDirBlaBle).ax().chkThis { strf.contains(bluName) } }

              "On blu file content" so {
                "it is empty" so { readFileWithCat(bluPath).ax().chkEmpty() }
                "On write poem" so {
                  val poem = listOf("NOTHING IS FAIR IN THIS WORLD OF MADNESS!")
                  writeFileWithDD(poem, bluPath).ax()
                  "poem is there" so { readFileWithCat(bluPath).ax() chkEq poem }
                  "On write empty list of lines" so {
                    writeFileWithDD(emptyList<String>(), bluPath).ax()
                    "it is empty again" so { readFileWithCat(bluPath).ax().chkEmpty() }
                  }
                }
              }

              "On rm blu" so {
                rm(bluPath).ax()

                "ls blu is NOT there" so { lsRegFiles(tmpDirBlaBle).ax().chkThis { !strf.contains(bluName) } }
              }

              "On rm wrong file name" so {
                "using nice wrapper outputs File not found" so {
                  rmFileIfExists("$bluPath.wrong".pth).ax().chkEq(listOf("File not found"))
                }
                "using plain rm throws BadExitStateErr".soThrows<BadExitStateErr> {
                  rm("$bluPath.wrong".pth).ax()
                }
              }
            }

            "On rmTreeWithForce" so {
              rmTreeWithForce(tmpDir) { path -> path.strf.startsWith("/tmp/testDirTmp") }.ax()

              "tmp does not contain our dir" so { lsSubDirs("/tmp".pth).ax().chkThis { !strf.contains(dirName) } }
            }

            "On konfig in tmpDir" so {
              val konfigNewDir = tmpDir / "tmpKonfigForTests"
              val konfig = konfigInDir(konfigNewDir, localCLI())

              testGivenNewKonfigInDir(konfig, konfigNewDir)
            }

          } finally {
            // Clean up. Notice: The "On rmTreeWithForce" above is only for specific test branch,
            // but here we always make sure we clean up in all uspek cases.
            rmTreeWithForce(tmpDir) { path -> path.strf.startsWith("/tmp/testDirTmp") }.ax()
          }
        }
      }
    }
  }
}

