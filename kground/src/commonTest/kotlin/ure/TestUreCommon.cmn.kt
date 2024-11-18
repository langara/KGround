@file:OptIn(NotPortableApi::class, DelicateApi::class)

package pl.mareklangiewicz.ure

import pl.mareklangiewicz.annotations.*
import pl.mareklangiewicz.udata.lO
import pl.mareklangiewicz.ure.bad.chkIR
import pl.mareklangiewicz.uspek.*


fun testUreCommonStuff() {

  "On ureIdent" o {
    ureIdent().chkIR("\\b[a-zA-Z_]\\w*\\b").tstMatchCorrectInputs(
      match = lO("bla", "Ble12", "bLu23ZZ", "_1", "__main__", "_", "i"),
      matchNot = lO("1ble", "1_", "+", ""),
      alsoCheckNegation = false,
      verbose = true,
    )
  }

  // TODO continue
}
