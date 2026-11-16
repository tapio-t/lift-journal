package com.example.liftjournal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The domain packages must stay pure Kotlin so it can be unit-tested on the JVM (CLAUDE.md). */
class DomainPurityTest {
    private val forbiddenPrefixes = listOf("import android.", "import androidx.")

    @Test
    fun `domain sources import nothing from Android`() {
        val domainDir = File("src/main/java/com/example/liftjournal/domain")
        assertTrue("domain sources not found at ${domainDir.absolutePath}", domainDir.isDirectory)

        val offenders = domainDir.walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines().filter { line -> forbiddenPrefixes.any(line::startsWith) }.map { "${file.name}: $it" }
            }
            .toList()

        assertEquals(emptyList<String>(), offenders)
    }
}
