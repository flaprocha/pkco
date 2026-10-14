package com.example.pkco.data.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryBuilderTest {
    private fun placeholders(sql: String) = sql.count { it == '?' }

    @Test
    fun words_splitOnAnyWhitespace_dropBlanks() {
        assertEquals(listOf("kotlin", "android"), SearchQueryBuilder.words("  kotlin \t\n  android  "))
    }

    @Test
    fun words_blankQuery_isEmpty() {
        assertEquals(emptyList<String>(), SearchQueryBuilder.words(""))
        assertEquals(emptyList<String>(), SearchQueryBuilder.words("   \t "))
    }

    @Test
    fun words_keepAtMostEight() {
        val words = SearchQueryBuilder.words("1 2 3 4 5 6 7 8 9 10")
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8"), words)
    }

    @Test
    fun escapeLike_escapesPercentUnderscoreAndBackslash() {
        assertEquals("50\\%", SearchQueryBuilder.escapeLike("50%"))
        assertEquals("a\\_b", SearchQueryBuilder.escapeLike("a_b"))
        assertEquals("c:\\\\x", SearchQueryBuilder.escapeLike("c:\\x"))
        assertEquals("\\\\\\%", SearchQueryBuilder.escapeLike("\\%"))
        assertEquals("plain", SearchQueryBuilder.escapeLike("plain"))
    }

    @Test
    fun build_wordArguments_areEscapedPatterns() {
        val query = SearchQueryBuilder.build("50%", null, null)
        assertEquals(List(4) { "%50\\%%" }, query.args)
        assertTrue(query.sql.contains("ESCAPE '\\'"))
    }

    @Test
    fun build_blankQuery_hasNoScoring_andOrdersByEdited() {
        val query = SearchQueryBuilder.build("  ", null, null)
        assertFalse(query.sql.contains("score"))
        assertFalse(query.sql.contains("LIKE"))
        assertTrue(query.sql.contains("deletedAt IS NULL"))
        assertTrue(query.sql.endsWith("ORDER BY n.updatedAt DESC, n.createdAt DESC"))
        assertEquals(0, placeholders(query.sql))
        assertEquals(emptyList<String>(), query.args)
    }

    @Test
    fun build_placeholdersMatchArguments_forZeroOneAndSeveralWords_withEachFilter() {
        val filters = listOf(null to null, "cat" to null, null to "tag", "cat" to "tag")
        for (text in listOf("", "kotlin", "kotlin android room", "1 2 3 4 5 6 7 8 9")) {
            for ((categoryId, tagId) in filters) {
                val query = SearchQueryBuilder.build(text, categoryId, tagId)
                val expected = 4 * SearchQueryBuilder.words(text).size + listOfNotNull(categoryId, tagId).size
                assertEquals("args for '$text', $categoryId, $tagId", expected, query.args.size)
                assertEquals("placeholders for '$text', $categoryId, $tagId", expected, placeholders(query.sql))
            }
        }
    }

    @Test
    fun build_filterArguments_comeAfterWordArguments_inSqlOrder() {
        val query = SearchQueryBuilder.build("kotlin", "cat-id", "tag-id")
        assertEquals(List(4) { "%kotlin%" } + listOf("cat-id", "tag-id"), query.args)
        assertTrue(query.sql.indexOf("n.categoryId = ?") < query.sql.indexOf("ft.tagId = ?"))
        assertTrue(query.sql.lastIndexOf("LIKE ?") < query.sql.indexOf("n.categoryId = ?"))
    }

    @Test
    fun build_sqlDependsOnlyOnWordCountAndFilters() {
        assertEquals(
            SearchQueryBuilder.build("alpha beta", "c1", null).sql,
            SearchQueryBuilder.build("x'OR--1=1   ;DROP", "other", null).sql
        )
        assertEquals(
            SearchQueryBuilder.build("", null, "t1").sql,
            SearchQueryBuilder.build("   ", null, "t2").sql
        )
    }

    @Test
    fun build_userTextNeverAppearsInSql() {
        val text = "zebra' DROP TABLE notes; -- 100%_off"
        val query = SearchQueryBuilder.build(text, "category'id", "tag\"id")
        for (word in SearchQueryBuilder.words(text)) {
            assertFalse("'$word' in SQL", query.sql.contains(word))
        }
        assertFalse(query.sql.contains("category'id"))
        assertFalse(query.sql.contains("tag\"id"))
        assertFalse(query.sql.contains("zebra"))
    }
}
