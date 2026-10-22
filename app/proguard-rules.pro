# kotlinx.serialization: keep generated serializers of @Serializable classes (content models, routes, settings JSON).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class nl.potentplum.mise.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class nl.potentplum.mise.** {
    kotlinx.serialization.KSerializer serializer(...);
}
