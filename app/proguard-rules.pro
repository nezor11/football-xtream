# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.footballxtream.data.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.footballxtream.data.**$$serializer { *; }
-keepclassmembers class com.footballxtream.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
