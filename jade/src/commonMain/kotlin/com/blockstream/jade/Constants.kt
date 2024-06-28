package com.blockstream.jade


// Timeouts for autonomous calls that should return quickly, calls that require user confirmation,
// and calls that need arbitrarily long (eg. entering a mnemonic) and should not timeout at all.
const val TIMEOUT_AUTONOMOUS: Int = 4_000 // 4 secs
const val TIMEOUT_AUTONOMOUS_LONG: Int = 8_000 // 8 secs
const val TIMEOUT_USER_INTERACTION = 120_000 // 2 mins
const val TIMEOUT_NONE = -1