package com.example.pkco.data.search

/** One SQL statement and its bind arguments, in the order of the `?` placeholders. */
data class SearchQuery(val sql: String, val args: List<String>)

/**
 * Builds the search query over `notes`. The SQL text depends only on the number of words and on which filters are
 * set; all user text goes in as bind arguments.
 *
 * Relevance, for each word (partial match, ignoring case): title 3 points, any tag name or the category name
 * 2 points (once), body 1 point. Points add up across words; notes with 0 points are left out. Order: score,
 * then updatedAt, then createdAt, highest first. A blank query has no scoring and keeps the list order.
 */
object SearchQueryBuilder {
    const val MAX_WORDS = 8

    private const val ORDER_BY_EDITED = "n.updatedAt DESC, n.createdAt DESC"

    /** The words of [query]: split on whitespace, blanks dropped, at most [MAX_WORDS]. */
    fun words(query: String): List<String> =
        query.split(Regex("\\s+")).filter { it.isNotEmpty() }.take(MAX_WORDS)

    /** Escapes `\`, `%` and `_` so the word matches literally in `LIKE ... ESCAPE '\'`. */
    fun escapeLike(word: String): String =
        word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    fun build(query: String, categoryId: String?, tagId: String?): SearchQuery {
        val words = words(query)
        val args = mutableListOf<String>()
        val filters = StringBuilder("n.deletedAt IS NULL")
        // Filter arguments are added after the score arguments, matching their place in the SQL text.
        val filterArgs = mutableListOf<String>()
        if (categoryId != null) {
            filters.append(" AND n.categoryId = ?")
            filterArgs += categoryId
        }
        if (tagId != null) {
            filters.append(" AND EXISTS (SELECT 1 FROM note_tags AS ft WHERE ft.noteId = n.id AND ft.tagId = ?)")
            filterArgs += tagId
        }

        if (words.isEmpty()) {
            args += filterArgs
            return SearchQuery("SELECT n.* FROM notes AS n WHERE $filters ORDER BY $ORDER_BY_EDITED", args)
        }

        val score = words.joinToString(" + ") { word ->
            val pattern = "%${escapeLike(word)}%"
            repeat(4) { args += pattern }
            "(CASE WHEN n.title LIKE ? ESCAPE '\\' THEN 3 ELSE 0 END" +
                " + CASE WHEN EXISTS (SELECT 1 FROM note_tags AS st JOIN tags AS t ON t.id = st.tagId" +
                " WHERE st.noteId = n.id AND t.name LIKE ? ESCAPE '\\')" +
                " OR EXISTS (SELECT 1 FROM categories AS c WHERE c.id = n.categoryId AND c.name LIKE ? ESCAPE '\\')" +
                " THEN 2 ELSE 0 END" +
                " + CASE WHEN n.body LIKE ? ESCAPE '\\' THEN 1 ELSE 0 END)"
        }
        args += filterArgs
        // The inner query scores each note; the outer one returns only the note columns, so Room maps them as usual.
        val sql = "SELECT n.* FROM notes AS n" +
            " JOIN (SELECT n.id AS scoredId, $score AS score FROM notes AS n WHERE $filters) AS r" +
            " ON r.scoredId = n.id" +
            " WHERE r.score > 0" +
            " ORDER BY r.score DESC, $ORDER_BY_EDITED"
        return SearchQuery(sql, args)
    }
}
