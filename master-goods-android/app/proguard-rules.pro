# Preserve annotations and generated serializer metadata used across modules.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep kotlinx.serialization generated classes stable under shrinking.
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class kotlinx.serialization.internal.** { *; }

# Keep Hilt / Dagger generated wiring intact.
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class *_HiltModules* { *; }
-keep class *Hilt* { *; }

# Keep Retrofit interfaces and model names that are reflected by tooling or serialization.
-keep interface com.zhihuiji.core.network.ZhihuijiApi { *; }
-keep class com.zhihuiji.core.model.** { *; }

# Reduce noise from optional annotations.
-dontwarn javax.annotation.**
