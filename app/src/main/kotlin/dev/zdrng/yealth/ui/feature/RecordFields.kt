package dev.zdrng.yealth.ui.feature

import dev.zdrng.yealth.domain.model.*

/** Flatten only presentation structure. Every leaf, explicit null and empty collection remains visible. */
internal fun flattenedFields(fields: List<RecordField>): List<RecordField> = buildList {
    fun visit(path: String, value: HealthValue) {
        when (value) {
            is HealthValue.Fields -> if (value.fields.isEmpty()) add(RecordField(path, HealthValue.Text("{}")))
                else value.fields.forEach { visit("$path · ${it.key}", it.value) }
            is HealthValue.Items -> if (value.values.isEmpty()) add(RecordField(path, HealthValue.Text("[]")))
                else value.values.forEachIndexed { index, item -> visit("$path [${index + 1}]", item) }
            else -> add(RecordField(path, value))
        }
    }
    fields.forEach { visit(it.key, it.value) }
}
