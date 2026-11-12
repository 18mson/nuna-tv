package com.nunatv.app.util

/**
 * Detektor kategori video otomatis berdasarkan metadata YouTube (kategori resmi, tag, judul, channel).
 */
object CategoryDetector {

    private data class CategoryRule(
        val targetCategory: String,
        val youtubeCategories: Set<String>,
        val keywords: List<String>
    )

    // Urutan penting: kategori spesifik (Anime, Anak, Musik, Gaming) dicek lebih dulu daripada Hiburan umum
    private val RULES = listOf(
        CategoryRule(
            targetCategory = "Anime",
            youtubeCategories = emptySet(),
            keywords = listOf(
                "anime", "sub indo", "donghua", "amv", "ova",
                "naruto", "one piece", "jujutsu kaisen", "kimetsu", "demon slayer",
                "attack on titan", "bleach", "dragon ball", "boruto", "solo leveling",
                "chainsaw man", "spy x family", "opening anime", "ending anime"
            )
        ),
        CategoryRule(
            targetCategory = "Anak",
            youtubeCategories = emptySet(),
            keywords = listOf(
                "lagu anak", "kartun", "cartoon", "kids", "untuk anak", "anak-anak",
                "upin ipin", "upin & ipin", "boboiboy", "doraemon", "cocomelon",
                "baby shark", "nursery rhyme", "dongeng", "mewarnai", "belajar huruf",
                "belajar berhitung", "tayo", "pororo", "masha", "tom and jerry"
            )
        ),
        CategoryRule(
            targetCategory = "Musik",
            youtubeCategories = setOf("music", "musik"),
            keywords = listOf(
                "official music video", "music video", "official video", "official audio",
                "lyric", "lirik", "lagu ", "cover", "acoustic", "akustik",
                "remix", "dj ", "konser", "concert", "live session", "karaoke",
                "sholawat", "qasidah", "nasyid", "band", "album", "ost "
            )
        ),
        CategoryRule(
            targetCategory = "Gaming",
            youtubeCategories = setOf("gaming", "game"),
            keywords = listOf(
                "gameplay", "walkthrough", "playthrough", "mabar", "esports",
                "mobile legends", "mlbb", "free fire", "pubg", "valorant",
                "minecraft", "roblox", "genshin", "honkai", "gta ", "efootball",
                "fifa", "fc 24", "fc 25", "steam", "ps5", "nintendo", "speedrun"
            )
        ),
        CategoryRule(
            targetCategory = "Edukasi",
            youtubeCategories = setOf(
                "education", "science & technology", "howto & style",
                "edukasi", "sains & teknologi", "cara & gaya"
            ),
            keywords = listOf(
                "tutorial", "belajar", "cara ", "kursus", "materi", "penjelasan",
                "coding", "programming", "python", "kotlin", "javascript", "android studio",
                "matematika", "fisika", "kimia", "biologi", "sejarah", "sains",
                "review", "unboxing", "resep", "memasak", "tips ", "edukasi", "dokumenter"
            )
        ),
        CategoryRule(
            targetCategory = "Berita",
            youtubeCategories = setOf("news & politics", "berita & politik"),
            keywords = listOf(
                "berita", "news", "breaking", "tvone", "kompas", "metro tv",
                "cnn", "bbc", "tribun", "liputan", "cnn indonesia", "cnbc",
                "live report", "konferensi pers", "wawancara", "politik"
            )
        ),
        CategoryRule(
            targetCategory = "Olahraga",
            youtubeCategories = setOf("sports", "olahraga"),
            keywords = listOf(
                "sepak bola", "football", "soccer", "highlights", "highlight",
                "liga ", "premier league", "champions league", "timnas", "garuda",
                "motogp", "formula 1", "f1 ", "badminton", "bulutangkis", "bwf",
                "nba", "ufc", "boxing", "tinju", "voli", "futsal"
            )
        ),
        CategoryRule(
            targetCategory = "Hiburan",
            youtubeCategories = setOf(
                "entertainment", "comedy", "film & animation", "people & blogs",
                "hiburan", "komedi", "film & animasi", "blog & orang"
            ),
            keywords = listOf(
                "podcast", "stand up", "komedi", "lucu", "ngakak", "vlog",
                "reaction", "trailer", "film ", "movie", "drama", "sinema",
                "talkshow", "prank", "tantangan", "challenge"
            )
        )
    )

    fun detectCategory(
        title: String,
        uploader: String = "",
        ytCategories: List<String>? = null,
        tags: List<String>? = null,
        description: String? = null,
        existingCategories: Set<String> = emptySet()
    ): String {
        val normalizedCustom = existingCategories
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("Umum", ignoreCase = true) }

        val titleLower = title.lowercase()
        val uploaderLower = uploader.lowercase()
        val tagsText = tags.orEmpty().joinToString(" ").lowercase()
        val descSnippet = description.orEmpty().take(400).lowercase()
        val primaryText = "$titleLower $uploaderLower $tagsText"
        val fullText = "$primaryText $descSnippet"
        val normalizedYtCats = ytCategories.orEmpty().map { it.trim().lowercase() }

        // 1. Prioritaskan kecocokan langsung dengan nama kategori kustom buatan pengguna
        val customMatch = normalizedCustom.firstOrNull { catName ->
            val needle = catName.lowercase()
            needle.length >= 3 && (primaryText.contains(needle) || normalizedYtCats.any { it.contains(needle) })
        }
        if (customMatch != null) {
            return customMatch
        }

        // 2. Cek kecocokan keyword spesifik (Anime & Anak diperiksa lebih dulu meski kategori YouTube Film/Entertainment)
        for (rule in RULES.take(2)) {
            if (matchesAnyKeyword(primaryText, descSnippet, rule.keywords)) {
                return resolveCaseWithExisting(rule.targetCategory, normalizedCustom)
            }
        }

        // 3. Cek kategori resmi YouTube (dari VideoInfo.categories yt-dlp)
        for (rule in RULES) {
            if (normalizedYtCats.any { ytCat -> rule.youtubeCategories.any { ytCat == it || ytCat.contains(it) } }) {
                return resolveCaseWithExisting(rule.targetCategory, normalizedCustom)
            }
        }

        // 4. Skor kecocokan keyword di judul, nama channel, tag, dan cuplikan deskripsi
        var bestRule: CategoryRule? = null
        var bestScore = 0
        for (rule in RULES) {
            var score = 0
            for (kw in rule.keywords) {
                if (titleLower.contains(kw)) score += 3
                else if (uploaderLower.contains(kw) || tagsText.contains(kw)) score += 2
                else if (fullText.contains(kw)) score += 1
            }
            if (score > bestScore) {
                bestScore = score
                bestRule = rule
            }
        }

        if (bestRule != null && bestScore >= 2) {
            return resolveCaseWithExisting(bestRule.targetCategory, normalizedCustom)
        }

        return "Umum"
    }

    private fun matchesAnyKeyword(primaryText: String, descSnippet: String, keywords: List<String>): Boolean {
        return keywords.any { kw -> primaryText.contains(kw) || descSnippet.contains(kw) }
    }

    private fun resolveCaseWithExisting(candidate: String, customCategories: List<String>): String {
        return customCategories.firstOrNull { it.equals(candidate, ignoreCase = true) } ?: candidate
    }
}
