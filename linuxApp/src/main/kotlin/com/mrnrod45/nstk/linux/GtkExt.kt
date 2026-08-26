package com.mrnrod45.nstk.linux

import org.gnome.gtk.Widget

/** Frame draws a border but adds zero internal padding — content sits flush against
 *  it otherwise, so anything going directly into a Frame needs this explicitly. */
fun <T : Widget> T.withMargin(margin: Int = 12): T {
    marginTop = margin
    marginBottom = margin
    marginStart = margin
    marginEnd = margin
    return this
}
