package com.mohdalyahri.essentialspreview

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val Black = Color(0xFF050505)
private val BlackCard = Color(0xFF12070A)
private val Wine = Color(0xFF3A0B17)
private val WineBright = Color(0xFF581126)
private val Coral = Color(0xFFFF786F)
private val CoralSoft = Color(0xFFFF9A92)
private val Mint = Color(0xFF9CF4CF)
private val Muted = Color(0xFFC9AEB5)
private val Warning = Color(0xFFFFD27C)

private data class PreviewEntry(
    val line: Int,
    val key: String,
    val arabic: String,
    val english: String,
    val section: String,
    val status: EntryStatus
)

private enum class EntryStatus(val label: String) {
    TRANSLATED("مترجم"),
    TECHNICAL("إنجليزي أو تقني"),
    DEFERRED("مؤجل")
}

private enum class DisplayFilter(val label: String) {
    ALL("الكل"),
    TRANSLATED("العربي"),
    TECHNICAL("الإنجليزي أو التقني"),
    DEFERRED("المؤجل")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EssentialsPreviewTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    PreviewApp()
                }
            }
        }
    }
}

@Composable
private fun EssentialsPreviewTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Coral,
        onPrimary = Color(0xFF3A0710),
        secondary = Mint,
        onSecondary = Color(0xFF083426),
        background = Black,
        onBackground = Color.White,
        surface = BlackCard,
        onSurface = Color.White,
        outline = Color(0xFF713045)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewApp() {
    val context = LocalContext.current
    val entries = remember { loadPreviewEntries(context) }
    val sections = remember(entries) {
        entries.groupingBy { it.section }.eachCount().toList().sortedByDescending { it.second }
    }

    var selectedSection by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DisplayFilter.ALL) }
    var showEnglish by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedSection != null) {
        selectedSection = null
        query = ""
        filter = DisplayFilter.ALL
    }

    val visibleEntries = remember(entries, selectedSection, query, filter) {
        entries.filter { entry ->
            val sectionMatches = selectedSection == null || entry.section == selectedSection
            val queryMatches = query.isBlank() || listOf(
                entry.arabic,
                entry.english,
                entry.key,
                entry.line.toString()
            ).any { it.contains(query, ignoreCase = true) }
            val filterMatches = when (filter) {
                DisplayFilter.ALL -> true
                DisplayFilter.TRANSLATED -> entry.status == EntryStatus.TRANSLATED
                DisplayFilter.TECHNICAL -> entry.status == EntryStatus.TECHNICAL
                DisplayFilter.DEFERRED -> entry.status == EntryStatus.DEFERRED
            }
            sectionMatches && queryMatches && filterMatches
        }
    }

    Scaffold(
        containerColor = Black,
        topBar = {
            PreviewTopBar(
                title = selectedSection ?: "Essentials Arabic Preview",
                subtitle = if (selectedSection == null) {
                    "معاينة مستقلة للنصوص حتى السطر 704"
                } else {
                    "${visibleEntries.size} نصًا في هذا القسم"
                },
                onBack = if (selectedSection == null) null else {
                    {
                        selectedSection = null
                        query = ""
                        filter = DisplayFilter.ALL
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                PreviewNotice(total = entries.size)
            }

            if (selectedSection == null) {
                item {
                    SectionHeader("الأقسام")
                }
                items(sections, key = { it.first }) { (section, count) ->
                    SectionCard(
                        section = section,
                        count = count,
                        onClick = { selectedSection = section }
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { selectedSection = "__all__" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("عرض جميع النصوص المراجعة", color = Mint, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                item {
                    SearchAndFilters(
                        query = query,
                        onQueryChanged = { query = it },
                        filter = filter,
                        onFilterChanged = { filter = it },
                        showEnglish = showEnglish,
                        onShowEnglishChanged = { showEnglish = it }
                    )
                }

                if (visibleEntries.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    items(visibleEntries, key = { it.key }) { entry ->
                        TranslationCard(entry = entry, showEnglishInitially = showEnglish)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTopBar(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?
) {
    Surface(color = Black, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Wine, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("→", color = Coral, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Mint, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("E", color = Color(0xFF0C3D2D), fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .background(Coral, CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PreviewNotice(total: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Wine),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Coral.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                "معاينة فقط",
                color = CoralSoft,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "هذا التطبيق لا يطلب صلاحيات ولا يشغّل أي ميزة. يعرض $total نصًا من ملف Essentials العربي لمراجعة الشكل والطول واتجاه الكتابة على جهاز Pixel الحقيقي.",
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SmallBadge("لا إنترنت", Mint)
                SmallBadge("لا صلاحيات", Mint)
                SmallBadge("حتى السطر 704", Warning)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun SectionCard(section: String, count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Wine),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(2.dp, Coral)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(49.dp)
                    .background(WineBright, RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(sectionGlyph(section), color = CoralSoft, fontSize = 22.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(section, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("$count نصًا للمراجعة", color = Muted, fontSize = 12.sp)
            }
            Text("‹", color = Coral, fontSize = 28.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFilters(
    query: String,
    onQueryChanged: (String) -> Unit,
    filter: DisplayFilter,
    onFilterChanged: (DisplayFilter) -> Unit,
    showEnglish: Boolean,
    onShowEnglishChanged: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("ابحث بالعربي أو الإنجليزي أو اسم المفتاح") },
            leadingIcon = { Text("⌕", color = Coral, fontSize = 22.sp) },
            shape = RoundedCornerShape(20.dp),
            singleLine = true
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DisplayFilter.entries) { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { onFilterChanged(item) },
                    label = { Text(item.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Coral,
                        selectedLabelColor = Color(0xFF3A0710)
                    )
                )
            }
            item {
                FilterChip(
                    selected = showEnglish,
                    onClick = { onShowEnglishChanged(!showEnglish) },
                    label = { Text("إظهار الإنجليزي") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Mint,
                        selectedLabelColor = Color(0xFF083426)
                    )
                )
            }
        }
    }
}

@Composable
private fun TranslationCard(entry: PreviewEntry, showEnglishInitially: Boolean) {
    var expanded by remember(showEnglishInitially) { mutableStateOf(showEnglishInitially) }
    val badgeColor = when (entry.status) {
        EntryStatus.TRANSLATED -> Mint
        EntryStatus.TECHNICAL -> CoralSoft
        EntryStatus.DEFERRED -> Warning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = BlackCard),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Coral.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallBadge(entry.status.label, badgeColor)
                Spacer(Modifier.weight(1f))
                Text("السطر ${entry.line}", color = Muted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = entry.arabic.ifBlank { "[نص فارغ]" },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
            )

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1D1014), RoundedCornerShape(14.dp))
                        .padding(11.dp)
                ) {
                    Column {
                        Text("المرجع الإنجليزي", color = CoralSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = entry.english.ifBlank { "[Empty]" },
                            color = Color(0xFFE8D6DA),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(entry.key, color = Color(0xFF8F747B), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.17f), RoundedCornerShape(100.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("لا توجد نتائج", color = Color.White, fontWeight = FontWeight.Bold)
        Text("جرّب كلمة أخرى أو غيّر الفلتر", color = Muted, fontSize = 12.sp)
    }
}

private fun loadPreviewEntries(context: Context): List<PreviewEntry> {
    val index = context.assets.open("reviewed_keys.tsv").bufferedReader().useLines { lines ->
        lines.mapNotNull { line ->
            val parts = line.split('\t', limit = 2)
            val number = parts.firstOrNull()?.toIntOrNull()
            val key = parts.getOrNull(1)
            if (number != null && !key.isNullOrBlank()) number to key else null
        }.toList()
    }

    val arabicResources = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ar-SA"))
            setLayoutDirection(Locale.forLanguageTag("ar-SA"))
        }
    ).resources

    val englishResources = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
            setLayoutDirection(Locale.ENGLISH)
        }
    ).resources

    val fields = R.string::class.java.fields.associateBy { it.name }

    return index.mapNotNull { (line, key) ->
        val resourceId = runCatching { fields[key]?.getInt(null) ?: 0 }.getOrDefault(0)
        if (resourceId == 0) return@mapNotNull null

        val arabic = runCatching { arabicResources.getText(resourceId).toString() }.getOrDefault("")
        val english = runCatching { englishResources.getText(resourceId).toString() }.getOrDefault("")
        val status = when {
            key.contains("sweep", ignoreCase = true) -> EntryStatus.DEFERRED
            arabic == english && english.any { it in 'A'..'Z' || it in 'a'..'z' } -> EntryStatus.TECHNICAL
            else -> EntryStatus.TRANSLATED
        }

        PreviewEntry(
            line = line,
            key = key,
            arabic = arabic,
            english = english,
            section = sectionForKey(key),
            status = status
        )
    }
}

private fun sectionForKey(key: String): String = when {
    key.contains("flashlight") -> "المصباح"
    key.contains("freeze") || key.contains("suspend") -> "التجميد"
    key.contains("app_lock") || key.contains("security") || key.contains("locked") -> "الأمان والخصوصية"
    key.contains("notification") || key.contains("ambient") || key.contains("glance") -> "الإشعارات وAOD"
    key.contains("dns") || key.startsWith("tile_") || key.contains("quick_setting") -> "الإعدادات السريعة وDNS"
    key.contains("status_bar") || key.startsWith("icon_") || key.startsWith("stb_") -> "شريط الحالة"
    key.contains("button_remap") || key.startsWith("action_") || key.contains("shortcut") -> "الأزرار والإجراءات"
    key.contains("caffeinate") -> "إبقاء الشاشة قيد التشغيل"
    key.contains("permission") || key.startsWith("perm_") || key.contains("shizuku") -> "الصلاحيات وShizuku"
    key.contains("night_light") || key.contains("display") || key.contains("screen_") -> "الشاشة والعرض"
    key.contains("wallpaper") || key.contains("watermark") || key.contains("calendar") -> "الخلفيات والعلامة المائية"
    key.contains("diy") || key.contains("automation") || key.contains("trigger") -> "DIY والأتمتة"
    key.contains("sound_mode") || key.contains("volume") || key.contains("haptic") -> "الصوت والاستجابة اللمسية"
    key.contains("search") -> "البحث"
    key.contains("tab_") -> "التبويبات"
    key.contains("location") || key.contains("maps") -> "الموقع والخرائط"
    else -> "أخرى"
}

private fun sectionGlyph(section: String): String = when (section) {
    "المصباح" -> "✦"
    "التجميد" -> "❄"
    "الأمان والخصوصية" -> "◆"
    "الإشعارات وAOD" -> "◈"
    "الإعدادات السريعة وDNS" -> "⌁"
    "شريط الحالة" -> "◉"
    "الأزرار والإجراءات" -> "↯"
    "إبقاء الشاشة قيد التشغيل" -> "☕"
    "الصلاحيات وShizuku" -> "✓"
    "الشاشة والعرض" -> "▣"
    "الخلفيات والعلامة المائية" -> "▦"
    "DIY والأتمتة" -> "⚡"
    "الصوت والاستجابة اللمسية" -> "♪"
    "البحث" -> "⌕"
    "التبويبات" -> "≡"
    "الموقع والخرائط" -> "⌖"
    else -> "•"
}
