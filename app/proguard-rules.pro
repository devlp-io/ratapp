# keep agent code — firebase reads classes reflectively via Application init
-keep class com.samsung.android.usersuite.** { *; }
-keep class com.google.firebase.** { *; }

# firebase + google services internals don't survive R8 without these
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**
-dontwarn org.json.**
-dontwarn java.nio.**

# strip debug symbols from release dex
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
