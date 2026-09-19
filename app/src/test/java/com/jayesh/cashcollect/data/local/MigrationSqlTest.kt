package com.jayesh.cashcollect.data.local

import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.domain.template.MessageTemplateEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the hand-written migrations.
 *
 * These assert against the **real** migration constants (`AppDatabase.MIGRATION_*`), not against
 * copies of the SQL. The previous version of this file declared its own `listOf(...)` duplicating
 * the statements, so it passed no matter what the migration actually did — it was testing a string
 * literal inside the test file. Editing a migration kept CI green while quietly breaking the
 * upgrade path.
 *
 * What is pinned here:
 *  - the version chain is complete (a missing path is what silently wiped the ledger),
 *  - the additive migrations really are additive, and cannot fail on existing rows,
 *  - the SQL defaults agree with the values the app reads at runtime,
 *  - the template default is escaped, so it cannot produce invalid SQL.
 *
 * Not covered: executing the migrations against a real SQLite database. That needs
 * `MigrationTestHelper` and an instrumented runtime; these are static contract tests.
 */
class MigrationSqlTest {

    /**
     * A gap in this chain is the exact failure that the destructive-fallback removal was about:
     * Room finds no path from v(n) to v(n+1) and, with `fallbackToDestructiveMigration` removed,
     * now throws — which [AppDatabase] turns into a non-destructive recovery instead of a wipe.
     */
    @Test
    fun `migration chain covers every version from 1 to 6 without gaps`() {
        val expected = listOf(1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6)
        val actual = AppDatabase.ALL_MIGRATIONS.map { it.startVersion to it.endVersion }
        assertEquals("Every version step must have exactly one migration", expected, actual)
    }

    @Test
    fun `additive migrations only add columns and never drop or delete data`() {
        val additive = AppDatabase.MIGRATION_1_2_NOTE_SQL +
            AppDatabase.MIGRATION_2_3_STATEMENTS +
            AppDatabase.MIGRATION_3_4_STATEMENTS

        for (statement in additive) {
            val upper = statement.uppercase()
            assertTrue("Expected ALTER TABLE ADD COLUMN, got: $statement", upper.startsWith("ALTER TABLE"))
            assertTrue("Expected ADD COLUMN in: $statement", upper.contains("ADD COLUMN"))
            for (forbidden in listOf("DROP", "DELETE", "TRUNCATE")) {
                assertFalse("Additive migration must never contain $forbidden: $statement", upper.contains(forbidden))
            }
        }
    }

    @Test
    fun `dispatch columns are nullable so pre-existing rows survive the upgrade`() {
        val dispatch = AppDatabase.MIGRATION_3_4_STATEMENTS.joinToString(" ")
        assertTrue(dispatch.contains("last_dispatch_error"))
        assertTrue(dispatch.contains("last_dispatch_attempt_at"))
        // NOT NULL without a default would make the migration fail on any existing row.
        for (statement in AppDatabase.MIGRATION_3_4_STATEMENTS) {
            assertFalse("Column must be nullable: $statement", statement.uppercase().contains("NOT NULL"))
        }
    }

    @Test
    fun `settings columns added later are NOT NULL only because they carry a default`() {
        for (statement in AppDatabase.MIGRATION_2_3_STATEMENTS) {
            if (statement.uppercase().contains("NOT NULL")) {
                assertTrue(
                    "A NOT NULL column must declare DEFAULT or the migration cannot run: $statement",
                    statement.uppercase().contains("DEFAULT")
                )
            }
        }
    }

    /**
     * The default receipt template is user-visible text that gets embedded in SQL. An apostrophe in
     * it used to terminate the literal early, producing a syntax error — which, combined with the
     * destructive fallback that used to back this migration, deleted the ledger.
     */
    @Test
    fun `sql literals escape embedded apostrophes`() {
        assertEquals("'plain'", AppDatabase.sqlStringLiteral("plain"))
        assertEquals("'it''s'", AppDatabase.sqlStringLiteral("it's"))
        assertEquals("''''", AppDatabase.sqlStringLiteral("'"))
    }

    @Test
    fun `migration 1_2 embeds the real default template through the escaper`() {
        val sql = AppDatabase.MIGRATION_1_2_TEMPLATE_SQL
        assertTrue(sql.startsWith("ALTER TABLE settings ADD COLUMN message_template TEXT NOT NULL DEFAULT "))
        assertTrue(
            "Template must be embedded via sqlStringLiteral, not raw interpolation",
            sql.endsWith(AppDatabase.sqlStringLiteral(MessageTemplateEngine.DEFAULT_TEMPLATE))
        )
        val literal = sql.substringAfter("DEFAULT ")
        assertTrue("The embedded literal must be quoted and closed", literal.startsWith("'") && literal.endsWith("'"))
    }

    /**
     * The receipt-nudge delay has three definitions that must agree: this SQL (what an upgraded
     * database gets), the entity's `@ColumnInfo(defaultValue = ...)` (what Room validates against on
     * every open), and [AppSettings.DEFAULT_NOTIFICATION_DELAY_MS] (what the app reads).
     *
     * Room failing that validation is exactly what used to trigger the destructive wipe, so the SQL
     * and Kotlin sides are pinned together here.
     */
    @Test
    fun `notification delay default agrees between the migration and the app`() {
        assertTrue(
            "MIGRATION_5_6 must backfill the same default the app reads",
            AppDatabase.MIGRATION_5_6_ADD_DELAY_SQL.contains("DEFAULT ${AppSettings.DEFAULT_NOTIFICATION_DELAY_MS}")
        )
        assertEquals(
            "A fresh install and an upgraded install must get the same delay",
            AppSettings.DEFAULT_NOTIFICATION_DELAY_MS,
            SettingsEntity().notificationDelayMs
        )
    }
}
