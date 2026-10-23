package nl.potentplum.mise.data.repo

import javax.inject.Inject
import javax.inject.Singleton

/** Imported recipes that are previewed or edited before "Bewaren" (spec §6.9). In memory only. */
@Singleton
class RecipeDrafts
    @Inject
    constructor() {
        private val drafts = mutableMapOf<String, Recipe>()

        @Synchronized
        fun put(recipe: Recipe) {
            drafts[recipe.id] = recipe
        }

        @Synchronized
        fun get(id: String): Recipe? = drafts[id]

        @Synchronized
        fun remove(id: String) {
            drafts.remove(id)
        }
    }
