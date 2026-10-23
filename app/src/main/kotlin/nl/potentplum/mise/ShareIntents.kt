package nl.potentplum.mise

import android.content.Context
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

    /** Not a share: a notification that opens one recipe. */
    data class OpenRecipe(
        val id: String,
    ) : SharedContent
}

object ShareIntents {
    private const val ACTION_OPEN_RECIPE = "nl.potentplum.mise.action.OPEN_RECIPE"
    private const val EXTRA_RECIPE_ID = "recipeId"

    fun openRecipe(
        context: Context,
        recipeId: String,
    ): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_RECIPE)
            .putExtra(EXTRA_RECIPE_ID, recipeId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

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

            ACTION_OPEN_RECIPE -> {
                intent.getStringExtra(EXTRA_RECIPE_ID)?.let(SharedContent::OpenRecipe)
            }

            else -> {
                null
            }
        }
    }
}
