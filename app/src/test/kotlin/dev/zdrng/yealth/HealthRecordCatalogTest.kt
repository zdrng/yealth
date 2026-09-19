package dev.zdrng.yealth

import androidx.health.connect.client.records.Record
import dev.zdrng.yealth.data.healthconnect.HealthRecordCatalog
import dev.zdrng.yealth.domain.model.*
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import org.junit.Assert.*
import org.junit.Test

class HealthRecordCatalogTest {
    private val catalog = HealthRecordCatalog()

    @Test fun `every catalog type has a localized browsing label`() {
        assertEquals(catalog.entries.map { it.type.id }.toSet(), dev.zdrng.yealth.ui.feature.typeLabels.keys)
    }

    @Test fun `every concrete SDK record is explicitly catalogued`() {
        // Reflection is test-only: an SDK bump must fail if the explicit production catalog drifts.
        val sdk = File(checkNotNull(Record::class.java.protectionDomain?.codeSource).location.toURI())
        val classes = JarFile(sdk).use { jar -> jar.entries().asSequence()
            .map { it.name }
            .filter { it.startsWith("androidx/health/connect/client/records/") && it.endsWith("Record.class") && '$' !in it }
            .map { Class.forName(it.removeSuffix(".class").replace('/', '.'), false, Record::class.java.classLoader) }
            .filter { Record::class.java.isAssignableFrom(it) && !it.isInterface && !Modifier.isAbstract(it.modifiers) }
            .map { it.name }.toSet()
        }
        assertEquals(classes, catalog.entries.map { it.recordClass.java.name }.toSet())
        assertEquals(41, classes.size)
    }

    @Test fun `catalog identities permissions categories and schemas are complete`() {
        assertEquals(catalog.entries.size, catalog.entries.map { it.type.id }.distinct().size)
        assertEquals(HealthCategory.entries.toSet(), catalog.entries.map { it.type.category }.toSet())
        for (entry in catalog.entries) {
            assertTrue(entry.type.readPermission.startsWith("android.permission.health.READ_"))
            val keys = entry.type.fields.map { it.key }
            assertEquals(keys.size, keys.distinct().size)
            assertTrue("metadata" in keys)
            assertTrue("time" in keys || ("startTime" in keys && "endTime" in keys))
        }
        val nutrition = catalog.entries.single { it.type.id == "nutrition" }.type
        assertTrue(nutrition.fields.any { it.key == "vitaminB12" && it.sdkValueType == "Mass?" })
        assertTrue(nutrition.fields.any { it.key == "energyFromFat" })
    }

    @Test fun `experimental types and unfinished readers cannot masquerade as implemented`() {
        val types = catalog.entries.map { it.type }
        assertEquals(40, types.count { it.readerStatus == ReaderStatus.IMPLEMENTED })
        assertEquals(ReaderStatus.EXPERIMENTAL_REVIEW, types.single { it.id == "mindfulness_session" }.readerStatus)
        assertEquals(HealthFeature.MINDFULNESS, types.single { it.id == "mindfulness_session" }.feature)
        assertEquals(HealthFeature.SKIN_TEMPERATURE, types.single { it.id == "skin_temperature" }.feature)
        assertEquals(HealthFeature.PLANNED_EXERCISE, types.single { it.id == "planned_exercise_session" }.feature)
    }

    @Test fun `record schemas account for every public SDK field`() {
        for (entry in catalog.entries) {
            val getters = entry.recordClass.java.methods.filter {
                it.declaringClass != Any::class.java && !Modifier.isStatic(it.modifiers) &&
                    it.parameterCount == 0 && (it.name.startsWith("get") || it.name == "hasExplicitTime")
            }.map {
                if (it.name == "hasExplicitTime") it.name
                else it.name.removePrefix("get").replaceFirstChar(Char::lowercase)
            }.toSet()
            assertEquals(entry.type.id, getters, entry.type.fields.map { it.key }.toSet())
        }
    }
}
