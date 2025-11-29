# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==========================================================
# FiaGoods Custom Rules
# ==========================================================

# 1. 保护 data 包下所有代码（不仅是 model，还包含 SupabaseApi 等）
-keep class com.glassous.fiagoods.data.** { *; }

# 2. 保留泛型签名信息 (对于 Gson 解析 List<Map<...>> 至关重要)
-keepattributes Signature

# 3. 保留注解信息 (确保 @SerializedName 生效)
-keepattributes *Annotation*

# 4. 保留内部类和封闭方法信息
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# 5. Gson 核心防混淆规则
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
# 额外加强：强制保留所有继承自 TypeToken 的类的所有成员
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# 6. 解决第三方库 R8 警告
-dontwarn com.google.errorprone.annotations.**