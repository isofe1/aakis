# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve Room Database Entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Keep models for data reflection
-keep class com.example.data.** { *; }
-keep class com.example.engine.** { *; }

# Preserve Line Numbers for Release Stack Traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
