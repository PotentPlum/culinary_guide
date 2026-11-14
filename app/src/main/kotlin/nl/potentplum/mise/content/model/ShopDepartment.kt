package nl.potentplum.mise.content.model

import kotlinx.serialization.Serializable

/** Mirrors `data/schemas/departments.schema.json`. */
@Serializable
data class DepartmentsFile(
    val schemaVersion: Int,
    val departments: List<ShopDepartment>,
)

/** A department of the shopping list (spec §6.14), in display order; its label is `shopping_department_<id>`. */
@Serializable
data class ShopDepartment(
    val id: String,
    /** Lowercase words without diacritics; see `DepartmentClassifier` for how they match. */
    val keywords: List<String>,
)
