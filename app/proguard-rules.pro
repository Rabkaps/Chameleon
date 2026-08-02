# Proguard rules for Chameleon

# 1. Chameleon Application, VPN Service & Data Models
-keep class com.hambalapps.chameleon.vpn.** { *; }
-keep class com.hambalapps.chameleon.data.** { *; }
-keep class com.hambalapps.chameleon.ui.** { *; }
-keep class com.hambalapps.chameleon.Config { *; }

# 2. Sing-box / Libbox Core Bindings (Package: io.nekohasekai.libbox)
-keep class io.nekohasekai.libbox.** { *; }
-keep interface io.nekohasekai.libbox.** { *; }
-keepclassmembers class io.nekohasekai.libbox.** { *; }
-keep class io.nekohasekai.** { *; }
-keep class libbox.** { *; }

# 3. Go Runtime (Gomobile JNI Bridge)
-keep class go.** { *; }
-keep interface go.** { *; }
-keepclassmembers class go.** { *; }

# 4. Native JNI Callbacks & Interfaces
-keepclasseswithmembernames class * {
    native <methods>;
}

# 5. Serialization Attributes, Annotations, and Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    synthetic com.hambalapps.chameleon.**$serializer INSTANCE;
}
-keep class com.hambalapps.chameleon.NavigationKeys* { *; }

# 6. JSON Models & Data Parsers (ConfigInjector outbounds/endpoints)
-keep class org.json.** { *; }
-keepclassmembers class org.json.** { *; }

# 7. Android DataStore & Preferences
-keep class androidx.datastore.** { *; }

# 8. ML Kit Barcode Scanning Keep Rules
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.odml.** { *; }
-keep class com.google.android.gms.common.internal.safeparcel.** { *; }
