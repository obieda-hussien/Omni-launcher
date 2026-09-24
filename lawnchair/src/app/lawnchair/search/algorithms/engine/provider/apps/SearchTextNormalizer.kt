package app.lawnchair.search.algorithms.engine.provider.apps

import java.text.Normalizer
import java.util.Locale

/** Normalization for offline app search only; does not send query/app names to a service. */
internal object SearchTextNormalizer {
    private val diacritics = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(diacritics, "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ٱ', 'ا')
        .replace('ى', 'ي')
        .replace('ـ', ' ')
        .trim()
}
