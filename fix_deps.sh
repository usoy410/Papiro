#!/bin/bash
sed -i '/kotlinxSerializationJson/d' gradle/libs.versions.toml
sed -i '/retrofitConverterSerialization/d' gradle/libs.versions.toml
sed -i '/kotlinx-serialization-json/d' gradle/libs.versions.toml
sed -i '/retrofit-converter-serialization/d' gradle/libs.versions.toml
sed -i '/kotlin-serialization/d' gradle/libs.versions.toml

# Insert versions
sed -i '/\[versions\]/a \
kotlinxSerializationJson = "1.6.3"\
retrofitConverterSerialization = "1.0.0"\
' gradle/libs.versions.toml

# Insert libraries
sed -i '/\[libraries\]/a \
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }\
retrofit-converter-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "retrofitConverterSerialization" }\
' gradle/libs.versions.toml

# Insert plugins
sed -i '/\[plugins\]/a \
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }\
' gradle/libs.versions.toml
