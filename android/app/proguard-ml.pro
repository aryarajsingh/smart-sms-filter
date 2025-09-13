# TensorFlow Lite specific rules
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keepclassmembers class * {
    native <methods>;
}

# Keep ML classifier classes
-keep class com.smartsmsfilter.ml.** { *; }

# Keep model assets
-keepattributes *Annotation*
-keepattributes Signature
-keep class * extends java.lang.Enum { *; }

# Additional rules for TensorFlow Lite delegates
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }
-keep class org.tensorflow.lite.hexagon.** { *; }