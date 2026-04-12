# Moshi
-keep class com.sasquatsh.app.data.remote.dto.** { *; }
-keep class com.sasquatsh.app.domain.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*

# Firebase
-keep class com.google.firebase.** { *; }
