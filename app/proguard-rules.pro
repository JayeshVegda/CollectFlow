# Add project specific ProGuard rules here.

# Room SQLite rules
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep our Room Entities, DAOs, and Database
-keep class com.jayesh.cashcollect.data.local.entity.** { *; }
-keep class com.jayesh.cashcollect.data.local.dao.** { *; }
-keep class com.jayesh.cashcollect.data.local.AppDatabase { *; }

# Keep Domain Models and Enums
-keep class com.jayesh.cashcollect.domain.model.** { *; }
-keep class com.jayesh.cashcollect.domain.state.CollectionStatus { *; }

# WorkManager
-keep class androidx.work.** { *; }
