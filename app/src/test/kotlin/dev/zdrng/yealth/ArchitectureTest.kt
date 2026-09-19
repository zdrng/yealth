package dev.zdrng.yealth

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ROOT_PACKAGE = "dev.zdrng.yealth"

class ArchitectureTest {
    private val sourceRoot: File = File("src/main/kotlin").takeIf(File::isDirectory)
        ?: File("app/src/main/kotlin").takeIf(File::isDirectory)
        ?: error("Kotlin source root not found")

    @Test
    fun `Health Connect and data implementations never leak into UI or domain`() {
        val violations = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val path = file.invariantSeparatorsPath
                file.readLines().mapIndexedNotNull { index, line ->
                    val sdkLeak = "androidx.health.connect." in line && "/data/healthconnect/" !in path
                    val uiLeak = "/ui/" in path && "$ROOT_PACKAGE.data." in line
                    val dataLeak = "/data/" in path && listOf("$ROOT_PACKAGE.ui.", "$ROOT_PACKAGE.di.").any(line::contains)
                    val viewModelLeak = file.name.endsWith("ViewModel.kt") && "$ROOT_PACKAGE.di." in line
                    if (sdkLeak || uiLeak || dataLeak || viewModelLeak) "${file.path}:${index + 1}: $line" else null
                }
            }.toList()
        assertTrue("Layer boundary violations:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    @Test
    fun `domain stays independent of frameworks and outer layers`() {
        val violations = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "/domain/" in it.invariantSeparatorsPath }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val imported = line.trim().removePrefix("import ")
                    val forbidden = line.trim().startsWith("import ") && listOf(
                        "android.",
                        "androidx.",
                        "$ROOT_PACKAGE.data.",
                        "$ROOT_PACKAGE.di.",
                        "$ROOT_PACKAGE.ui.",
                    ).any(imported::startsWith)
                    if (forbidden) "${file.path}:${index + 1}: $line" else null
                }
            }
            .toList()

        assertTrue("Forbidden domain imports:\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
