-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.chatglobal.app.**$$serializer { *; }
-keepclassmembers class com.chatglobal.app.** { *** Companion; }
-keepclasseswithmembers class com.chatglobal.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn kotlinx.coroutines.**
-keep class com.chatglobal.app.data.model.** { *; }
-dontwarn androidx.compose.**
