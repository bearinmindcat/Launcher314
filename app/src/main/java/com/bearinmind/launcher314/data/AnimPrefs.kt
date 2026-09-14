package com.bearinmind.launcher314.data

import android.content.Context
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap

/** Issue #111: process-wide mirror of "Reduce animations" — a pref read per icon per composition would cost more than the animations it drops. */
object AnimPrefs {
    @Volatile
    var reduce: Boolean = false

    fun refresh(context: Context) {
        reduce = getReduceAnimations(context)
    }
}

/** [spec] normally, an instant snap when Reduce animations is on. */
fun <T> lessAnim(spec: AnimationSpec<T>): AnimationSpec<T> = if (AnimPrefs.reduce) snap() else spec
