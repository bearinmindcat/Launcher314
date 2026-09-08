package com.bearinmind.launcher314.ui.drawer

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.bearinmind.launcher314.data.AppFolder
import com.bearinmind.launcher314.data.AppInfo
import com.bearinmind.launcher314.data.folderEntry
import com.bearinmind.launcher314.data.getInstalledApps
import com.bearinmind.launcher314.data.nestedFolderIds
import com.bearinmind.launcher314.helpers.rememberHapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Drawer tabs — user categories as a chip row in the drawer (Neo style). "All" is always first; tap to switch, long-press a custom chip to edit, "+" to create. */
@Serializable
data class DrawerTab(
    val id: String,
    val name: String,
    val packages: List<String> = emptyList(),
    // Locked tabs require the user's password (stored only as a SHA-256 hash) before viewing or editing.
    val locked: Boolean = false,
    val passwordHash: String? = null
)

/** SHA-256 hex of a password — we never store the plaintext. */
fun hashTabPassword(password: String): String {
    val bytes = java.security.MessageDigest.getInstance("SHA-256")
        .digest(password.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

/** Set-password dialog: type it twice; onConfirm gets the SHA-256 hash, the caller stores it with locked = true. */
@Composable
fun SetTabPasswordDialog(onConfirm: (hash: String) -> Unit, onDismiss: () -> Unit) {
    var pw1 by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    val mismatch = pw2.isNotEmpty() && pw1 != pw2
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E1E1E), tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Set a password", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    "This password will be required to open the tab.",
                    color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pw1, onValueChange = { pw1 = it },
                    label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pw2, onValueChange = { pw2 = it },
                    label = { Text("Confirm password") }, singleLine = true,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                // Reserved fixed-height slot so the error never resizes the dialog card.
                Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.CenterStart) {
                    if (mismatch) {
                        Text("Passwords don't match", color = Color(0xFFFF6B6B), fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancel", fontSize = 13.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { onConfirm(hashTabPassword(pw1)) },
                        enabled = pw1.isNotEmpty() && pw1 == pw2,
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Lock", fontSize = 13.sp) }
                }
            }
        }
    }
}

/** Unlock prompt: onSuccess when the hash matches; legacy locked tabs with no stored password fall through. */
@Composable
fun UnlockTabDialog(tab: DrawerTab, onSuccess: () -> Unit, onDismiss: () -> Unit) {
    var pw by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    if (tab.passwordHash == null) { onSuccess(); return }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E1E1E), tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Unlock ${tab.name}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pw,
                    onValueChange = { pw = it; wrong = false },
                    label = { Text("Password") }, singleLine = true,
                    isError = wrong,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                // Reserved fixed-height slot so the error can't resize the card.
                Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.CenterStart) {
                    if (wrong) Text("Wrong password", color = Color(0xFFFF6B6B), fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancel", fontSize = 13.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (hashTabPassword(pw) == tab.passwordHash) onSuccess() else wrong = true
                        },
                        enabled = pw.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Unlock", fontSize = 13.sp) }
                }
            }
        }
    }
}


private const val PREFS_NAME = "app_drawer_settings"
private const val KEY_DRAWER_TABS = "drawer_tabs_json"
private const val KEY_SELECTED_TAB = "drawer_selected_tab"
private const val KEY_TABS_ENABLED = "drawer_tabs_enabled"

fun isDrawerTabsEnabled(context: Context): Boolean {
    return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_TABS_ENABLED, true)
}

fun setDrawerTabsEnabled(context: Context, enabled: Boolean) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_TABS_ENABLED, enabled).apply()
}

// "Hide added apps from all": tabbed apps leave the All tab and can only live in one tab; search still spans everything.
private const val KEY_HIDE_TABBED_FROM_ALL = "drawer_tabs_hide_from_all"

fun isHideTabbedAppsFromAll(context: Context): Boolean {
    return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_HIDE_TABBED_FROM_ALL, false)
}

fun setHideTabbedAppsFromAll(context: Context, hide: Boolean) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_HIDE_TABBED_FROM_ALL, hide).apply()
}

// Put the tab chip row under the app grid instead of above it (Nova style).
private const val KEY_TABS_AT_BOTTOM = "drawer_tabs_at_bottom"

fun isTabsAtBottom(context: Context): Boolean {
    return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_TABS_AT_BOTTOM, false)
}

fun setTabsAtBottom(context: Context, enabled: Boolean) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_TABS_AT_BOTTOM, enabled).apply()
}

// Swipe horizontally between tabs (Neo/Nova style) — scroll mode only, paged mode owns horizontal swipes.
private const val KEY_SWIPE_TABS = "drawer_tabs_swipe"

fun isSwipeTabsEnabled(context: Context): Boolean {
    return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_SWIPE_TABS, false)
}

fun setSwipeTabsEnabled(context: Context, enabled: Boolean) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_SWIPE_TABS, enabled).apply()
}

// Chip row style: alignment, per-chip app count, and whether the "+" chip shows.
private const val KEY_CENTER_CHIPS = "drawer_tabs_center_chips" // legacy bool, migrated
private const val KEY_TAB_ALIGNMENT = "drawer_tabs_alignment"     // legacy 3-way 0/1/2
private const val KEY_TAB_ALIGNMENT_LR = "drawer_tabs_align_lr"   // 2-way: 0 = Left, 1 = Right
private const val KEY_SHOW_COUNTS = "drawer_tabs_show_counts"
private const val KEY_HIDE_PLUS = "drawer_tabs_hide_plus"

// Alignment is Left (0) / Right (1) only — legacy 3-way values migrate once (old Right 2 -> 1, else Left).
fun getTabAlignment(context: Context): Int {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.contains(KEY_TAB_ALIGNMENT_LR)) {
        return prefs.getInt(KEY_TAB_ALIGNMENT_LR, 0).coerceIn(0, 1)
    }
    val legacy = prefs.getInt(KEY_TAB_ALIGNMENT, 0)
    val migrated = if (legacy >= 2) 1 else 0
    prefs.edit().putInt(KEY_TAB_ALIGNMENT_LR, migrated).apply()
    return migrated
}

fun setTabAlignment(context: Context, alignment: Int) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putInt(KEY_TAB_ALIGNMENT_LR, alignment.coerceIn(0, 1)).apply()
}

fun isHidePlusChip(context: Context): Boolean {
    return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_HIDE_PLUS, false)
}

fun setHidePlusChip(context: Context, hide: Boolean) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_HIDE_PLUS, hide).apply()
}

fun isShowTabCounts(context: Context): Boolean {
    return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_SHOW_COUNTS, false)
}

fun setShowTabCounts(context: Context, show: Boolean) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_SHOW_COUNTS, show).apply()
}

private val tabsJson = Json { ignoreUnknownKeys = true }

fun loadDrawerTabs(context: Context): List<DrawerTab> {
    val raw = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_DRAWER_TABS, null) ?: return emptyList()
    return try { tabsJson.decodeFromString(raw) } catch (_: Exception) { emptyList() }
}

fun saveDrawerTabs(context: Context, tabs: List<DrawerTab>) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(KEY_DRAWER_TABS, tabsJson.encodeToString(tabs)).apply()
}

fun getSelectedDrawerTabId(context: Context): String? {
    return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_SELECTED_TAB, null)
}

// Default tab the drawer opens on (issue #96): null = last used, "" = All, else a tab id.
private const val KEY_DEFAULT_TAB = "drawer_default_tab"

fun getDefaultDrawerTabId(context: Context): String? {
    return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_DEFAULT_TAB, null)
}

fun setDefaultDrawerTabId(context: Context, id: String?) {
    return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().apply {
            if (id == null) remove(KEY_DEFAULT_TAB) else putString(KEY_DEFAULT_TAB, id)
        }.apply()
}

fun setSelectedDrawerTabId(context: Context, id: String?) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().apply {
            if (id == null) remove(KEY_SELECTED_TAB) else putString(KEY_SELECTED_TAB, id)
        }.apply()
}

/** Tab-drag end handling: moved commits the reorder, hold-in-place opens the editor — called from BOTH onDragEnd and onDragCancel (a chip click can turn an end into a cancel). */
private fun finishTabDrag(
    id: String?,
    moved: Boolean,
    liveTabs: List<DrawerTab>,
    savedTabs: List<DrawerTab>,
    onTabsChanged: (List<DrawerTab>) -> Unit,
    openEdit: (DrawerTab) -> Unit,
    openUnlock: (DrawerTab) -> Unit
) {
    if (id == null) return
    if (moved) {
        if (liveTabs.map { it.id } != savedTabs.map { it.id }) onTabsChanged(liveTabs)
    } else {
        val t = savedTabs.firstOrNull { it.id == id } ?: return
        if (t.locked) openUnlock(t) else openEdit(t)
    }
}

/** Official Compose animatePlacement recipe — chips slide to new slots on reshuffle; [animate] false lets the dragged chip snap and follow the finger. */
private fun Modifier.animatePlacement(animate: Boolean): Modifier = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { targetOffset = it.positionInParent().round() }
        .offset {
            val anim = animatable ?: Animatable(targetOffset, IntOffset.VectorConverter)
                .also { animatable = it }
            if (anim.targetValue != targetOffset) {
                scope.launch {
                    if (animate) {
                        anim.animateTo(targetOffset, spring(stiffness = Spring.StiffnessMediumLow))
                    } else {
                        anim.snapTo(targetOffset)
                    }
                }
            }
            anim.value - targetOffset
        }
}

/** The chip row: [All] [tab] [tab] ... [+] — self-contained, hosts its own create/edit dialog state. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DrawerTabRow(
    tabs: List<DrawerTab>,
    selectedTabId: String?,
    allApps: List<AppInfo>,
    allFolders: List<AppFolder> = emptyList(),
    onTabSelected: (String?) -> Unit,
    onTabsChanged: (List<DrawerTab>) -> Unit
) {
    // null = no dialog; DrawerTab with empty id = creating a new tab
    var editingTab by remember { mutableStateOf<DrawerTab?>(null) }
    // Pending unlock: authenticate this tab, then open its editor.
    var unlockingTab by remember { mutableStateOf<DrawerTab?>(null) }
    val tabRowContext = LocalContext.current
    val haptics = rememberHapticFeedback()

    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
    val fillSelected = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val labelColor = MaterialTheme.colorScheme.onSurface
    val chipShape = RoundedCornerShape(50)

    // Per-chip (offset, width), read by the auto-scroll effect to keep the active chip on-screen (issue #62).
    val chipPositions = remember { mutableStateMapOf<String, Pair<Int, Int>>() }

    @Composable
    fun TabChip(
        label: String,
        selected: Boolean,
        positionKey: String,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null,
        // Drag-to-reorder visuals (custom tabs only) — the gesture itself lives on the row, not here.
        translationX: Float = 0f,
        lifted: Boolean = false
    ) {
        // Smoothly pop bigger when picked up (long-pressed), like an app icon.
        val liftScale by animateFloatAsState(
            targetValue = if (lifted) 1.18f else 1f,
            label = "chipLift"
        )
        Box(
            modifier = Modifier
                // Record this chip's offset + width so the row can auto-scroll it into view (issue #62).
                .onGloballyPositioned { coords ->
                    chipPositions[positionKey] = coords.positionInParent().x.roundToInt() to coords.size.width
                }
                .zIndex(if (lifted) 1f else 0f)
                // Slide into place on reshuffle; the dragged chip snaps (animate = false) to follow the finger.
                .animatePlacement(animate = !lifted)
                .graphicsLayer {
                    this.translationX = translationX
                    scaleX = liftScale
                    scaleY = liftScale
                    if (lifted) {
                        shadowElevation = 10.dp.toPx()
                        alpha = 0.97f
                    }
                }
                .clip(chipShape)
                .background(if (selected) fillSelected else Color.Transparent, chipShape)
                .border(1.dp, outline, chipShape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    // Chip row style prefs — re-read when the drawer is recreated after a trip to Settings.
    val tabAlignment = remember { getTabAlignment(tabRowContext) }
    val showCounts = remember { isShowTabCounts(tabRowContext) }
    val hidePlus = remember { isHidePlusChip(tabRowContext) }
    val chipScroll = rememberScrollState()

    // Drag-to-reorder state — STABLE holders: the row's pointerInput never restarts, so remember(tabs) would go stale.
    var draggingId by remember { mutableStateOf<String?>(null) }
    val liveTabsState = remember { mutableStateOf(tabs) }
    LaunchedEffect(tabs) { if (draggingId == null) liveTabsState.value = tabs }
    var liveTabs by liveTabsState
    val currentTabs by rememberUpdatedState(tabs)
    val currentOnTabsChanged by rememberUpdatedState(onTabsChanged)
    var dragFingerX by remember { mutableFloatStateOf(0f) }   // finger x in row-content space
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var dragInitialSlotLeft by remember { mutableFloatStateOf(0f) } // grabbed chip's slot at pickup
    var dragMoved by remember { mutableStateOf(false) }
    // After a drop, the chip springs from where the finger let go back to its slot.
    var settlingId by remember { mutableStateOf<String?>(null) }
    val settleAnim = remember { Animatable(0f) }
    val settleScope = rememberCoroutineScope()

    // The currently-selected chip, and its recorded (offset, width).
    val selectedKey = selectedTabId ?: "__all__"
    val selectedPos = chipPositions[selectedKey]

    val chipAlignment = if (tabAlignment == 1) Alignment.End else Alignment.Start

    // On drop / cancel: spring the chip back into its slot, then commit the order or open the editor.
    val releaseDrag = {
        val id = draggingId
        if (id != null) {
            val residual = dragInitialSlotLeft + (dragFingerX - dragStartX) -
                (chipPositions[id]?.first?.toFloat() ?: 0f)
            settlingId = id
            settleScope.launch {
                settleAnim.snapTo(residual)
                settleAnim.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
                if (settlingId == id) settlingId = null
            }
        }
        finishTabDrag(id, dragMoved, liveTabs, currentTabs, currentOnTabsChanged, { editingTab = it }, { unlockingTab = it })
        draggingId = null
        dragMoved = false
    }

    // Always-scrollable strip; inner row min-width = viewport so alignment holds until chips overflow (issue #62).
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        val viewportWidthPx = with(LocalDensity.current) { maxWidth.roundToPx() }

        // When the selected tab changes (tap or swipe), centre it in the viewport.
        LaunchedEffect(selectedKey, selectedPos, viewportWidthPx, draggingId) {
            if (draggingId != null) return@LaunchedEffect   // don't fight a reorder drag
            val pos = selectedPos ?: return@LaunchedEffect
            if (viewportWidthPx <= 0) return@LaunchedEffect
            val (left, width) = pos
            val target = (left - (viewportWidthPx - width) / 2).coerceIn(0, chipScroll.maxValue)
            chipScroll.animateScrollTo(target)
        }

        Row(
            modifier = Modifier
                .horizontalScroll(chipScroll)
                .widthIn(min = maxWidth)
                // ONE long-press-drag gesture on the whole row (not per-chip) so reordering can't tear down the in-flight gesture; shuffles liveTabs live, commits on drop.
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val hit = liveTabs.firstOrNull { t ->
                                val p = chipPositions[t.id]
                                p != null && offset.x >= p.first && offset.x <= p.first + p.second
                            }
                            if (hit != null) {
                                draggingId = hit.id
                                dragMoved = false
                                dragStartX = offset.x
                                dragFingerX = offset.x
                                dragInitialSlotLeft = (chipPositions[hit.id]?.first ?: 0).toFloat()
                                // Buzz on pickup, like long-pressing an app icon.
                                haptics.performLongPress()
                            } else {
                                draggingId = null
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val id = draggingId ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            dragFingerX += dragAmount.x
                            if (abs(dragFingerX - dragStartX) > viewConfiguration.touchSlop) {
                                dragMoved = true
                            }
                            val dragged = liveTabs.firstOrNull { it.id == id }
                                ?: return@detectDragGesturesAfterLongPress
                            val others = liveTabs.filter { it.id != id }
                            var insert = others.size
                            for (i in others.indices) {
                                val p = chipPositions[others[i].id] ?: continue
                                val center = p.first + p.second / 2f
                                if (dragFingerX < center) { insert = i; break }
                            }
                            val rebuilt = others.toMutableList().also { it.add(insert, dragged) }
                            if (rebuilt.map { it.id } != liveTabs.map { it.id }) {
                                liveTabs = rebuilt
                            }
                        },
                        // Same handling for onDragEnd and onDragCancel — a chip click can turn an end into a cancel, so both must handle the edit case.
                        onDragEnd = { releaseDrag() },
                        onDragCancel = { releaseDrag() }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp, chipAlignment)
        ) {
        TabChip(
            label = "All",
            selected = selectedTabId == null,
            positionKey = "__all__",
            onClick = { onTabSelected(null) }
        )
        liveTabs.forEach { tab ->
            // key(tab.id): identity-stable so a reorder MOVES the node instead of recreating it.
            key(tab.id) {
                // No lock glyph on the drawer chip — keep it looking like a normal tab.
                val baseLabel = if (showCounts) "${tab.name} (${tab.packages.size})" else tab.name
                val isDragging = tab.id == draggingId
                val isSettling = tab.id == settlingId
                val isEditingThis = editingTab?.id == tab.id || unlockingTab?.id == tab.id
                // Follow the finger's MOVEMENT from the grab point (rendered pos = grabSlot + fingerDelta), continuous across reshuffles.
                val slotLeft = (chipPositions[tab.id]?.first ?: 0).toFloat()
                val translation = when {
                    isDragging -> (dragInitialSlotLeft + (dragFingerX - dragStartX)) - slotLeft
                    isSettling -> settleAnim.value   // spring back into the slot after drop
                    else -> 0f
                }
                TabChip(
                    label = baseLabel,
                    selected = selectedTabId == tab.id,
                    positionKey = tab.id,
                    onClick = { onTabSelected(tab.id) },
                    // No onLongClick — the row-level gesture owns long-press.
                    translationX = translation,
                    // Stay enlarged while dragging, springing back, or while this tab's editor is open.
                    lifted = isDragging || isSettling || isEditingThis
                )
            }
        }
        if (!hidePlus) {
            TabChip(
                label = "+",
                selected = false,
                positionKey = "__plus__",
                onClick = { editingTab = DrawerTab(id = "", name = "") }
            )
        }
        }
    }

    editingTab?.let { tab ->
        DrawerTabEditDialog(
            tab = tab,
            allApps = allApps,
            allFolders = allFolders,
            excludedPackages = if (isHideTabbedAppsFromAll(tabRowContext)) {
                tabs.filter { it.id != tab.id }.flatMap { it.packages }.toSet()
            } else emptySet(),
            onSave = { saved ->
                val updated = if (tab.id.isEmpty()) {
                    tabs + saved.copy(id = java.util.UUID.randomUUID().toString())
                } else {
                    tabs.map { if (it.id == tab.id) saved else it }
                }
                onTabsChanged(updated)
                editingTab = null
            },
            onDelete = {
                if (tab.id.isNotEmpty()) onTabsChanged(tabs.filter { it.id != tab.id })
                editingTab = null
            },
            onDismiss = { editingTab = null }
        )
    }

    unlockingTab?.let { tab ->
        UnlockTabDialog(
            tab = tab,
            onSuccess = { editingTab = tab; unlockingTab = null },
            onDismiss = { unlockingTab = null }
        )
    }
}

/** Create/edit a tab: name it and tick the apps that belong in it. */
@Composable
private fun DrawerTabEditDialog(
    tab: DrawerTab,
    allApps: List<AppInfo>,
    onSave: (DrawerTab) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    // Packages in OTHER tabs while "Hide added apps from all" is on — removed so an app lives in one tab.
    excludedPackages: Set<String> = emptySet(),
    // Top-level drawer folders, assignable to the tab as "folder:<id>" entries.
    allFolders: List<AppFolder> = emptyList()
) {
    val isNew = tab.id.isEmpty()
    var name by remember { mutableStateOf(tab.name) }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(tab.packages.toSet()) }
    var locked by remember { mutableStateOf(tab.locked) }
    var passwordHash by remember { mutableStateOf(tab.passwordHash) }
    var showSetPassword by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSaveAuth by remember { mutableStateOf(false) }

    // Build the tab as it would be saved from the current dialog state.
    fun buildTab() = tab.copy(
        name = name.trim().ifEmpty { "Tab" },
        packages = selected.toList(),
        locked = locked,
        passwordHash = if (locked) passwordHash else null
    )

    // Saving a LOCKED tab's changes requires the password once more.
    fun attemptSave() {
        val edited = buildTab()
        val changed = edited != tab
        if (tab.locked && tab.passwordHash != null && changed) {
            showSaveAuth = true
        } else {
            onSave(edited)
        }
    }

    // De-dupe (work-profile clones share a package) and sort for the picker.
    val pickerApps = remember(allApps, excludedPackages) {
        allApps.distinctBy { it.packageName }
            .filter { it.packageName !in excludedPackages }
            .sortedBy { it.name.lowercase() }
    }
    val shownApps = remember(pickerApps, search) {
        if (search.isBlank()) pickerApps
        else pickerApps.filter { it.name.contains(search, ignoreCase = true) }
    }
    val pickerCtx = LocalContext.current
    val pickerIconShape = remember { com.bearinmind.launcher314.data.getGlobalIconShape(pickerCtx) }
    val pickerIconBgColor = remember { com.bearinmind.launcher314.data.getGlobalIconBgColor(pickerCtx) }

    // Folders are offered above the apps; nested ones belong to their parent.
    val shownFolders = remember(allFolders, search) {
        val nested = nestedFolderIds(allFolders)
        allFolders.filter { it.id !in nested }
            .filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (isNew) "New tab" else "Edit tab",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tab name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search apps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Privacy lock — this tab will require fingerprint / PIN to open.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock this tab",
                            color = Color.White.copy(alpha = 0.87f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Requires fingerprint or PIN to open",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = locked,
                        onCheckedChange = { on ->
                            if (on) {
                                // Turning ON requires setting a password first.
                                showSetPassword = true
                            } else {
                                locked = false
                                passwordHash = null
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${selected.size} selected",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(shownFolders, key = { "folder_${it.id}" }) { folder ->
                        val entry = folderEntry(folder.id)
                        val checked = entry in selected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selected = if (checked) selected - entry else selected + entry
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            MiniFolderBox(
                                folder = folder,
                                allApps = allApps,
                                size = 32.dp,
                                iconClipShape = com.bearinmind.launcher314.helpers.getIconShape(pickerIconShape),
                                iconBgColor = pickerIconBgColor,
                                globalIconShapeName = pickerIconShape
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = folder.name,
                                color = Color.White.copy(alpha = 0.87f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selected = if (checked) selected - entry else selected + entry
                                }
                            )
                        }
                    }
                    items(shownApps, key = { it.packageName }) { app ->
                        val checked = app.packageName in selected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selected = if (checked) selected - app.packageName
                                    else selected + app.packageName
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            AsyncImage(
                                model = File(app.iconPath),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = app.name,
                                color = Color.White.copy(alpha = 0.87f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selected = if (checked) selected - app.packageName
                                    else selected + app.packageName
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Delete / Cancel / Save in equal-weight centered slots; a new tab has no Delete.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isNew) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFEF9A9A)
                                )
                            ) {
                                Text("Delete", fontSize = 13.sp)
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDDDDDD)
                            )
                        ) {
                            Text("Cancel", fontSize = 13.sp)
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { attemptSave() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showSetPassword) {
        SetTabPasswordDialog(
            onConfirm = { hash ->
                passwordHash = hash
                locked = true
                showSetPassword = false
            },
            onDismiss = {
                // Cancelled — leave the tab unlocked.
                showSetPassword = false
                locked = false
                passwordHash = null
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            message = "Delete tab \"${tab.name}\"?",
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showSaveAuth) {
        // Re-enter the password to confirm saving changes to a locked tab.
        UnlockTabDialog(
            tab = tab,
            onSuccess = { showSaveAuth = false; onSave(buildTab()) },
            onDismiss = { showSaveAuth = false }
        )
    }
}

/** Confirm-delete dialog matching DeviceAudioEQ's preset-delete popup. */
@Composable
fun ConfirmDeleteDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "Delete"
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF252525),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 16.dp)) {
                Text(title, color = Color(0xFFE2E2E2), fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, color = Color(0xFFAAAAAA), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF444444), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF9A9A)
                        ),
                        modifier = Modifier.weight(1f).padding(end = 3.dp)
                    ) { Text("Delete", fontSize = 14.sp) }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDDDDDD)
                        ),
                        modifier = Modifier.weight(1f).padding(start = 3.dp)
                    ) { Text("Cancel", fontSize = 14.sp) }
                }
            }
        }
    }
}

/** Full-screen tab manager (Settings → Drawer Tabs → "Manage Tab Settings") — same CRUD as the in-drawer chips. */
@Composable
fun ManageDrawerTabsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tabs by remember { mutableStateOf(loadDrawerTabs(context)) }
    val drawerFolders = remember { com.bearinmind.launcher314.data.loadDrawerData(context).folders }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var editingTab by remember { mutableStateOf<DrawerTab?>(null) }
    var unlockingTab by remember { mutableStateOf<DrawerTab?>(null) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { getInstalledApps(context) }
    }

    fun commit(updated: List<DrawerTab>) {
        tabs = updated
        saveDrawerTabs(context, updated)
    }

    var hideTabbedFromAll by remember { mutableStateOf(isHideTabbedAppsFromAll(context)) }
    var swipeTabs by remember { mutableStateOf(isSwipeTabsEnabled(context)) }
    var tabsBottom by remember { mutableStateOf(isTabsAtBottom(context)) }
    var showCounts by remember { mutableStateOf(isShowTabCounts(context)) }
    var hidePlus by remember { mutableStateOf(isHidePlusChip(context)) }
    var defaultTabId by remember { mutableStateOf(getDefaultDrawerTabId(context)) }
    var showDefaultTabPicker by remember { mutableStateOf(false) }
    var tabAlignment by remember { mutableStateOf(getTabAlignment(context).toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Manage Tab Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        // Everything below the header scrolls as one list, padded past the nav bar (3-button or gestures).
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                com.bearinmind.launcher314.ui.settings.SettingsToggleItem(
                    title = "Hide added apps from all",
                    subtitle = "Apps only exist one at a time",
                    checked = hideTabbedFromAll,
                    onCheckedChange = {
                        hideTabbedFromAll = it
                        setHideTabbedAppsFromAll(context, it)
                    }
                )
            }
            item {
                com.bearinmind.launcher314.ui.settings.SettingsToggleItem(
                    title = "Swipe between tabs",
                    subtitle = "Only works when rows are disabled",
                    checked = swipeTabs,
                    onCheckedChange = {
                        swipeTabs = it
                        setSwipeTabsEnabled(context, it)
                    }
                )
            }
            item {
                com.bearinmind.launcher314.ui.settings.SettingsToggleItem(
                    title = "Tabs at bottom",
                    subtitle = "Moves the tab bar below the app list",
                    checked = tabsBottom,
                    onCheckedChange = {
                        tabsBottom = it
                        setTabsAtBottom(context, it)
                    }
                )
            }
            item {
                com.bearinmind.launcher314.ui.settings.SettingsToggleItem(
                    title = "Show app count",
                    subtitle = "Show the number of apps on each tab chip",
                    checked = showCounts,
                    onCheckedChange = {
                        showCounts = it
                        setShowTabCounts(context, it)
                    }
                )
            }
            item {
                com.bearinmind.launcher314.ui.settings.SettingsToggleItem(
                    title = "Hide (+) in drawer",
                    subtitle = "New tabs can only be added from this screen",
                    checked = hidePlus,
                    onCheckedChange = {
                        hidePlus = it
                        setHidePlusChip(context, it)
                    }
                )
            }
            item {
                val defaultTabLabel = when {
                    defaultTabId == null -> "Last used"
                    defaultTabId!!.isEmpty() -> "All"
                    else -> tabs.firstOrNull { it.id == defaultTabId }?.name ?: "Last used"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDefaultTabPicker = true }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Default tab",
                            color = Color.White.copy(alpha = 0.87f),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "The tab the drawer opens on",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = defaultTabLabel,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }
            item {
                // Tab alignment — same thumb-drag slider + tick style as the drawer sliders.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)
                ) {
                    com.bearinmind.launcher314.ui.components.ThumbDragHorizontalSlider(
                        currentValue = tabAlignment,
                        config = com.bearinmind.launcher314.ui.components.SliderConfigs.tabAlignment,
                        onValueChange = {
                            tabAlignment = it
                            setTabAlignment(context, it.roundToInt())
                        },
                        onValueChangeFinished = {
                            setTabAlignment(context, tabAlignment.roundToInt())
                        }
                    )
                }
            }
            item {
                // Divider + accent section header between the settings and the tab list (Neo style).
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color.White.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tabs",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            items(tabs, key = { it.id }) { tab ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (tab.locked) unlockingTab = tab else editingTab = tab
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tab.name,
                            color = Color.White.copy(alpha = 0.87f),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${tab.packages.size} apps",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp
                        )
                    }
                    if (tab.locked) {
                        // Lock indicator on the RIGHT — plain padlock, no outline.
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = "Locked",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            // "+" add button under the last tab, styled like Recorder314's outlined square "+".
            items(listOf("add_tab_button")) {
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp, top = 8.dp, bottom = 16.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { editingTab = DrawerTab(id = "", name = "") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color.White.copy(alpha = 0.87f),
                        fontSize = 22.sp
                    )
                }
            }
        }
    }

    if (showDefaultTabPicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDefaultTabPicker = false }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF252525)
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "Default tab",
                        color = Color(0xFFE2E2E2),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    val options = buildList {
                        add(null to "Last used")
                        add("" to "All")
                        tabs.filter { !it.locked }.forEach { add(it.id to it.name) }
                    }
                    options.forEach { (id, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    defaultTabId = id
                                    setDefaultDrawerTabId(context, id)
                                    showDefaultTabPicker = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = defaultTabId == id,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.87f),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    editingTab?.let { tab ->
        DrawerTabEditDialog(
            tab = tab,
            allApps = apps,
            allFolders = drawerFolders,
            excludedPackages = if (isHideTabbedAppsFromAll(context)) {
                tabs.filter { it.id != tab.id }.flatMap { it.packages }.toSet()
            } else emptySet(),
            onSave = { saved ->
                commit(
                    if (tab.id.isEmpty()) {
                        tabs + saved.copy(id = java.util.UUID.randomUUID().toString())
                    } else {
                        tabs.map { if (it.id == tab.id) saved else it }
                    }
                )
                editingTab = null
            },
            onDelete = {
                if (tab.id.isNotEmpty()) commit(tabs.filter { it.id != tab.id })
                editingTab = null
            },
            onDismiss = { editingTab = null }
        )
    }

    unlockingTab?.let { tab ->
        UnlockTabDialog(
            tab = tab,
            onSuccess = { editingTab = tab; unlockingTab = null },
            onDismiss = { unlockingTab = null }
        )
    }
}
