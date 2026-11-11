# kotlinx.serialization: keep generated serializers for the backup JSON classes.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.example.liftjournal.domain.backup.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
