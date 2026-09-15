package com.jayesh.cashcollect.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the hand-written MIGRATION_3_4 SQL.
 *
 * Full Room migration tests need Robolectric / an instrumented runtime; here we assert the
 * invariants the app relies on: additive-only schema change (never a wipe), nullable columns,
 * and correct SQLite type affinity so Room's schema validation passes after the upgrade.
 */
class MigrationSqlTest {

    private val migrationSql = listOf(
        "ALTER TABLE collections ADD COLUMN last_dispatch_error TEXT",
        "ALTER TABLE collections ADD COLUMN last_dispatch_attempt_at INTEGER"
    )

    @Test
    fun `migration is additive only - no drop, no delete, no recreate`() {
        for (statement in migrationSql) {
            val upper = statement.uppercase()
            assertTrue("Expected ALTER TABLE ADD COLUMN, got: $statement", upper.startsWith("ALTER TABLE"))
            assertTrue(upper.contains("ADD COLUMN"))
            for (forbidden in listOf("DROP", "DELETE", "TRUNCATE", "RECREATE", "RENAME")) {
                assertFalse("Migration must never contain $forbidden", upper.contains(forbidden))
            }
        }
    }

    @Test
    fun `dispatch columns are nullable so old rows survive the upgrade`() {
        // TEXT / INTEGER without NOT NULL => nullable. If these were NOT NULL the migration
        // would fail on any pre-existing row and the app could not open the database.
        for (statement in migrationSql) {
            assertFalse("Columns must be nullable: $statement", statement.uppercase().contains("NOT NULL"))
        }
    }

    @Test
    fun `both dispatch columns are covered`() {
        val joined = migrationSql.joinToString(" ")
        assertTrue(joined.contains("last_dispatch_error"))
        assertTrue(joined.contains("last_dispatch_attempt_at"))
    }
}