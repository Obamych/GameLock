package com.example.gamelock.data.remote

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class SourceLang { ENGLISH, CHINESE, JAPANESE, OTHER }

object TranslationService {

    private val enRuTranslator by lazy {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.RUSSIAN)
            .build()
        Translation.getClient(options)
    }

    private val zhRuTranslator by lazy {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.RUSSIAN)
            .build()
        Translation.getClient(options)
    }

    private val jaRuTranslator by lazy {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.JAPANESE)
            .setTargetLanguage(TranslateLanguage.RUSSIAN)
            .build()
        Translation.getClient(options)
    }

    private var enAvailable = false
    private var zhAvailable = false
    private var jaAvailable = false

    fun detectLanguage(text: String): SourceLang {
        var cjk = 0
        var kana = 0
        var hangul = 0
        var cyrillic = 0
        var latin = 0
        val sample = text.take(200)
        for (c in sample) {
            when {
                c in '\u3040'..'\u309F' || c in '\u30A0'..'\u30FF' -> kana++
                c in '\u4E00'..'\u9FFF' || c in '\u3400'..'\u4DBF' || c in '\u2E80'..'\u2EFF' || c in '\u3000'..'\u303F' -> cjk++
                c in '\uAC00'..'\uD7AF' || c in '\u1100'..'\u11FF' || c in '\u3130'..'\u318F' -> hangul++
                c in '\u0400'..'\u04FF' || c in '\u0500'..'\u052F' -> cyrillic++
                c in 'a'..'z' || c in 'A'..'Z' -> latin++
            }
        }
        if (kana > 2) return SourceLang.JAPANESE
        if (cjk > latin * 2 && cjk > 5) return SourceLang.CHINESE
        if (hangul > latin * 2) return SourceLang.OTHER
        if (cyrillic > latin) return SourceLang.OTHER
        if (latin > 5) return SourceLang.ENGLISH
        return SourceLang.OTHER
    }

    suspend fun translate(text: String, lang: SourceLang): Result<String> {
        return try {
            val (translator, available) = when (lang) {
                SourceLang.ENGLISH -> enRuTranslator to enAvailable
                SourceLang.CHINESE -> zhRuTranslator to zhAvailable
                SourceLang.JAPANESE -> jaRuTranslator to jaAvailable
                SourceLang.OTHER -> return Result.failure(Exception("Unsupported language"))
            }
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
            val result = translator.translate(text).await()
            when (lang) {
                SourceLang.ENGLISH -> enAvailable = true
                SourceLang.CHINESE -> zhAvailable = true
                SourceLang.JAPANESE -> jaAvailable = true
                SourceLang.OTHER -> {}
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e("TranslationService", "Translate error (${lang.name}): ${e.message}")
            when (lang) {
                SourceLang.ENGLISH -> enAvailable = false
                SourceLang.CHINESE -> zhAvailable = false
                SourceLang.JAPANESE -> jaAvailable = false
                SourceLang.OTHER -> {}
            }
            Result.failure(e)
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
