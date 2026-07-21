# EggyHub Release 混淆与精简规则

# ---- OkHttp / Okio ----
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okio.** { *; }

# ---- 数据层模型 ----
# 项目使用手工 org.json 解析，模型类通过代码直接引用，理论上 R8 不会误删；
# 此处显式保留以防万一（应用体量小，对包体影响可忽略）。
-keep class com.timome.eggyhub.data.** { *; }

# ---- 崩溃日志可读化 ----
# 保留源文件名与行号，使 CrashActivity / Logcat 中的堆栈更易定位。
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
