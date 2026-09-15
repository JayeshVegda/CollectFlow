# Optimization & Warnings
-dontwarn **
-ignorewarnings

# Keep Annotations & Signatures for reflection/serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Room Database & Generated Impls
-keep class androidx.room.** { *; }
-keep class com.jayesh.cashcollect.data.local.** { *; }
-keep class com.jayesh.cashcollect.data.local.entity.** { *; }
-keep class com.jayesh.cashcollect.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Jetpack Security & Google Tink
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-keep class com.google.errorprone.annotations.** { *; }
-keep class javax.annotation.** { *; }

# Keep Domain Models and Enums
-keep class com.jayesh.cashcollect.domain.model.** { *; }
-keep class com.jayesh.cashcollect.domain.state.CollectionStatus { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }
-keep class com.jayesh.cashcollect.service.reminder.** { *; }

# Keep TDLib JNI bindings and generated models
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**
-keep class io.github.tdlibandroid.ktx.** { *; }

# Keep ZXing QR Code
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
