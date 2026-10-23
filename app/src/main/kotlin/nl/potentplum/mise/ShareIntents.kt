package nl.potentplum.mise

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import nl.potentplum.mise.ingest.SharedContentParser

/** What another app shared into Mise (spec §6.9). */
sealed interface SharedContent {
    data class Link(
        val url: String,
    ) : SharedContent

    data class Images(
        val uris: List<Uri>,
    ) : SharedContent
}

object ShareIntents {
    /** Text with a link, or one or more images; anything else is ignored. */
    fun read(intent: Intent?): SharedContent? {
        if (intent == null) return null
        val type = intent.type.orEmpty()
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                when {
                    type.startsWith("text/") -> {
                        intent.getStringExtra(Intent.EXTRA_TEXT)?.let(SharedContentParser::firstUrl)?.let(SharedContent::Link)
                    }

                    type.startsWith("image/") -> {
                        IntentCompat
                            .getParcelableExtra(
                                intent,
                                Intent.EXTRA_STREAM,
                                Uri::class.java,
                            )?.let { SharedContent.Images(listOf(it)) }
                    }

                    else -> {
                        null
                    }
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                if (type.startsWith("image/")) {
                    IntentCompat
                        .getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { SharedContent.Images(it) }
                } else {
                    null
                }
            }

            else -> {
                null
            }
        }
    }
}
