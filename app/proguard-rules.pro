# JanzFlare Release Hardening (R8 Obfuscation & Keep Rules)

-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Keep all Firestore @Keep data models & properties intact for CustomClassMapper
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.data.model.** { *; }

# Keep Google Credential Manager & Google Identity classes
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep Firebase & Firestore serialization helpers
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keepattributes *Annotation*

# OkHttp / Media3 / Coil safety
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keep class androidx.media3.** { *; }

# Google Mobile Ads (AdMob) SDK
-keep public class com.google.android.gms.ads.** { public *; }
-keep public class com.google.ads.** { public *; }
-dontwarn com.google.android.gms.ads.**

