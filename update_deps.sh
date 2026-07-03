#!/bin/bash
echo "kotlinxSerializationJson = \"1.8.0\"" >> gradle/libs.versions.toml
echo "retrofitConverterSerialization = \"2.11.0\"" >> gradle/libs.versions.toml
echo "kotlinx-serialization-json = { group = \"org.jetbrains.kotlinx\", name = \"kotlinx-serialization-json\", version.ref = \"kotlinxSerializationJson\" }" >> gradle/libs.versions.toml
echo "retrofit-converter-serialization = { group = \"com.squareup.retrofit2\", name = \"converter-kotlinx-serialization\", version.ref = \"retrofitConverterSerialization\" }" >> gradle/libs.versions.toml
echo "kotlin-serialization = { id = \"org.jetbrains.kotlin.plugin.serialization\", version.ref = \"kotlin\" }" >> gradle/libs.versions.toml
