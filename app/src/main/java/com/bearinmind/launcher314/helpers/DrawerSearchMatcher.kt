package com.bearinmind.launcher314.helpers

import com.bearinmind.launcher314.data.AppInfo
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Type-to-find app-drawer search — a word-boundary-aware subsequence scorer.
 *
 * Finds "YouTube" from "yt", "F-Droid" from "fd", "Gmail" from "gm", etc. This
 * is the standard fuzzy-initials matcher (fzf / VS Code / Sublime shape),
 * implemented from first principles. Behavior/tuning were validated against a
 * clean-room analysis of a well-known launcher's fallback matcher, but this is
 * an independent implementation — not a port of anyone's source.
 *
 * Scoring is a subsequence match with three quality tiers:
 *   - streak (each char continues the previous match)  -> 0 penalty
 *   - fresh hit at a word boundary (initials style)     -> 1 penalty
 * A perfect match sits near Int.MAX_VALUE; every word-boundary jump costs 1, so
 * tighter matches rank higher. A QWERTY typo-neighbour fallback rescues full
 * misses on queries of length >= 3, always ranked strictly below real matches.
 */
object DrawerSearchMatcher {

    private const val WORD_BOUNDARY_PENALTY = 1
    // Reserved tier — unreachable given the match entry condition (kept for
    // clarity / future headroom); a mid-word fresh hit can't occur.
    private const val MID_WORD_PENALTY = 8
    // Typo-fallback results are shifted down by this so they always rank below
    // any real match. Real ~ MAX_VALUE - small; typo ~ MAX_VALUE - 2^30.
    private const val TYPO_TIER_OFFSET = (1 shl 30) + 999   // 1_073_742_823
    private const val TYPO_FALLBACK_MIN_QUERY_LEN = 3
    private const val RELEVANCE_FLOOR_DIVISOR = 2

    private val WHITESPACE = Regex("\\s")
    private val collator: Collator by lazy { Collator.getInstance() }

    // Physical QWERTY neighbours for the typo fallback. Includes the number row
    // (near-letter neighbours) so version-numbered apps tolerate fat-fingers.
    private val TYPO_NEIGHBORS: Map<Char, CharArray> = mapOf(
        'q' to charArrayOf('w', 'a', 's'),
        'w' to charArrayOf('q', 'e', 'a', 's', 'd'),
        'e' to charArrayOf('w', 'r', 's', 'd'),
        'r' to charArrayOf('e', 't', 'd', 'f'),
        't' to charArrayOf('r', 'y', 'f', 'g'),
        'y' to charArrayOf('t', 'u', 'g', 'h'),
        'u' to charArrayOf('y', 'i', 'h', 'j'),
        'i' to charArrayOf('u', 'o', 'j', 'k'),
        'o' to charArrayOf('i', 'p', 'k', 'l'),
        'p' to charArrayOf('o', 'l'),
        'a' to charArrayOf('q', 'w', 's', 'z'),
        's' to charArrayOf('a', 'w', 'e', 'd', 'z', 'x'),
        'd' to charArrayOf('s', 'e', 'r', 'f', 'x', 'c'),
        'f' to charArrayOf('d', 'r', 't', 'g', 'c', 'v'),
        'g' to charArrayOf('f', 't', 'y', 'h', 'v', 'b'),
        'h' to charArrayOf('g', 'y', 'u', 'j', 'b', 'n'),
        'j' to charArrayOf('h', 'u', 'i', 'k', 'n', 'm'),
        'k' to charArrayOf('j', 'i', 'o', 'l', 'm'),
        'l' to charArrayOf('k', 'o', 'p'),
        'z' to charArrayOf('a', 's', 'x'),
        'x' to charArrayOf('z', 's', 'd', 'c'),
        'c' to charArrayOf('x', 'd', 'f', 'v'),
        'v' to charArrayOf('c', 'f', 'g', 'b'),
        'b' to charArrayOf('v', 'g', 'h', 'n'),
        'n' to charArrayOf('b', 'h', 'j', 'm'),
        'm' to charArrayOf('n', 'j', 'k'),
        '1' to charArrayOf('q', 'a'),
        '2' to charArrayOf('w', 'z'),
        '3' to charArrayOf('e'),
        '4' to charArrayOf('r'),
        '5' to charArrayOf('t'),
        '6' to charArrayOf('y', 'z'),
        '7' to charArrayOf('u'),
        '8' to charArrayOf('i'),
        '9' to charArrayOf('o'),
        '0' to charArrayOf('p'),
    )

    private class MatchState(
        var titleIndex: Int = -1,
        var queryIndex: Int = 0,
        var lastWasNotLetter: Boolean = false,
        var lastWasUpperRun: Int = 0,
        var nonUpperSinceLastUpper: Boolean = false,
        var lastWasMatch: Boolean = false,
        var matchStrength: Int = Int.MAX_VALUE,
    )

    /** query char == label char (already lowercased) under accent-folding rules. */
    private fun charsEquivalent(queryChar: Char, labelCharLower: Char): Boolean {
        if (queryChar == labelCharLower) return true
        // ASCII fast-path: no accent decomposition possible, skip the allocation.
        if (queryChar.code < 128 && labelCharLower.code < 128) return false
        val decomposed = Normalizer.normalize(labelCharLower.toString(), Normalizer.Form.NFD)
        return decomposed.isNotEmpty() && queryChar == decomposed[0]
        // CJK (Hiragana<->Katakana, Hangul jamo) can be added here later.
    }

    /** Feed one label char to the running state; returns true if it consumed a query char. */
    private fun step(state: MatchState, query: String, labelChar: Char, strictMode: Boolean): Boolean {
        if (state.queryIndex >= query.length) return false
        if (labelChar.isWhitespace()) {
            state.nonUpperSinceLastUpper = true
            state.lastWasNotLetter = true
            state.lastWasUpperRun = 0
            state.lastWasMatch = false
            return false
        }
        val prevMatch = state.lastWasMatch
        val prevSeenLower = state.nonUpperSinceLastUpper
        state.nonUpperSinceLastUpper = prevSeenLower || !labelChar.isUpperCase()

        val atWordBoundary =
            (!labelChar.isLowerCase() && state.nonUpperSinceLastUpper) ||
                !labelChar.isLetter() ||
                state.lastWasNotLetter ||
                (state.nonUpperSinceLastUpper && state.lastWasUpperRun > 1)

        if (labelChar.isUpperCase()) {
            state.lastWasUpperRun++
            state.nonUpperSinceLastUpper = false
        } else {
            state.lastWasUpperRun = 0
            state.nonUpperSinceLastUpper = true
        }
        state.lastWasNotLetter = !labelChar.isLetter()

        val labelLower = labelChar.lowercaseChar()
        val matches = (prevMatch || strictMode || atWordBoundary) &&
            charsEquivalent(query[state.queryIndex], labelLower)

        if (matches) {
            val penalty = when {
                prevMatch || strictMode -> 0
                atWordBoundary -> WORD_BOUNDARY_PENALTY
                else -> MID_WORD_PENALTY // unreachable given the entry condition
            }
            state.matchStrength -= penalty
            state.lastWasMatch = true
            state.queryIndex++
            return true
        }
        state.lastWasMatch = false
        return false
    }

    /** Score `query` (already normalized) against `label`. 0 = no match; higher is better. */
    fun scoreMatch(label: CharSequence, query: String, allowTypoFallback: Boolean = true): Int {
        if (label.length < query.length || query.isEmpty()) return 0

        val candidates = ArrayList<MatchState>()
        var current = MatchState()

        // Phase 1: seed a candidate wherever the first query char can anchor —
        // anywhere at label pos 0 (strict), and at word boundaries after.
        for (i in label.indices) {
            if (step(current, query, label[i], strictMode = i == 0)) {
                current.titleIndex = i
                candidates.add(current)
                current = MatchState()
            }
        }

        // Phase 2: extend every candidate in parallel (strict never applies here).
        if (candidates.isNotEmpty()) {
            for (i in label.indices) {
                for (c in candidates) {
                    if (c.queryIndex < query.length && c.titleIndex < i) {
                        step(c, query, label[i], strictMode = false)
                    }
                }
            }
        }

        val direct = candidates
            .filter { it.queryIndex >= query.length }
            .maxOfOrNull { it.matchStrength } ?: 0

        if (direct != 0 || !allowTypoFallback || query.length < TYPO_FALLBACK_MIN_QUERY_LEN) {
            return direct
        }

        // QWERTY typo fallback: substitute one neighbour per position, recurse.
        var best = 0
        val buf = query.toCharArray()
        for (i in query.indices) {
            val neighbors = TYPO_NEIGHBORS[query[i]] ?: continue
            val original = buf[i]
            for (n in neighbors) {
                buf[i] = n
                val s = scoreMatch(label, String(buf), allowTypoFallback = false) - TYPO_TIER_OFFSET
                if (s > best) best = s
            }
            buf[i] = original
        }
        return best
    }

    /**
     * Search the app list. Returns apps best-first, weak matches dropped
     * (score < maxScore / 2). A query containing '.' also matches package names
     * (exact, no typo tolerance) — a power-user shortcut, e.g. "com.google".
     *
     * @param fuzziness 0..100 leniency. 0 = strict (only tight streak matches,
     *   no typo tolerance); 100 = loose (allow scattered matches + typos).
     *   Controls the max "spread" (word-boundary jumps) a real match may have
     *   and whether the QWERTY typo fallback runs.
     */
    fun searchApps(apps: List<AppInfo>, rawQuery: String, fuzziness: Int = 50): List<AppInfo> {
        val q = WHITESPACE.replace(rawQuery.lowercase(Locale.ROOT), "")
        if (q.isEmpty()) return emptyList()

        val f = fuzziness.coerceIn(0, 100)
        val typoEnabled = f >= 30
        // How many word-boundary jumps a real match may span (0 = pure streaks
        // only). Score = MAX_VALUE - spread, so spread = MAX_VALUE - score.
        val maxSpread = Math.round(f / 100f * 10f)

        val packageFallback = '.' in q
        val pkgQuery = if (q.startsWith('.')) q.substring(1) else q

        var maxScore = 0
        val scored = ArrayList<Pair<AppInfo, Int>>(apps.size)
        for (app in apps) {
            var s = scoreMatch(app.name, q, allowTypoFallback = typoEnabled)
            if (s == 0 && packageFallback) {
                s = scoreMatch(app.packageName, pkgQuery, allowTypoFallback = false)
            }
            if (s <= 0) continue
            // Real matches sit just below MAX_VALUE; drop those too scattered for
            // the current strictness. Typo-fallback matches live far lower and are
            // gated by typoEnabled above, not this spread cap.
            val spread = Int.MAX_VALUE - s
            if (spread in 0..100_000 && spread > maxSpread) continue
            scored.add(app to s)
            if (s > maxScore) maxScore = s
        }

        val floor = maxScore / RELEVANCE_FLOOR_DIVISOR
        return scored
            .filter { it.second >= floor }
            .sortedWith(
                compareByDescending<Pair<AppInfo, Int>> { it.second }
                    .thenComparator { a, b -> collator.compare(a.first.name, b.first.name) }
            )
            .map { it.first }
    }
}
