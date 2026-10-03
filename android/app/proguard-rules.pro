# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.salehtl.dubainownext.**$$serializer { *; }
-keepclassmembers class com.salehtl.dubainownext.** { *** Companion; }
-keepclasseswithmembers class com.salehtl.dubainownext.** { kotlinx.serialization.KSerializer serializer(...); }
