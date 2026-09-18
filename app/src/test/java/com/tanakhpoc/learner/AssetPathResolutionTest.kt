package com.tanakhpoc.learner

import com.tanakhpoc.learner.data.PackRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.GZIPOutputStream
import java.io.ByteArrayOutputStream

/**
 * Guards catalog/glosses open paths against aapt2's habit of gunzipping `*.gz`
 * assets and stripping the `.gz` suffix in the packaged APK.
 */
class AssetPathResolutionTest {

    @Test
    fun assetCandidates_prefersGzThenPlain_dedupes() {
        val c = PackRepository.assetCandidates(
            "data/glosses.json.gz",
            "data/glosses.json",
            "glosses.json.gz",
            "data/glosses.json.gz",
            null,
            "  "
        )
        assertEquals(
            listOf("data/glosses.json.gz", "data/glosses.json"),
            c
        )
    }

    @Test
    fun readAssetText_fallsBackWhenGzMissing_plainJson() {
        val files = mapOf("data/glosses.json" to """{"H1":{"id":"H1","primary":"x"}}""")
        val text = PackRepository.readAssetText(
            { path ->
                files[path]?.byteInputStream()
                    ?: throw FileNotFoundException(path)
            },
            "data/glosses.json.gz",
            "data/glosses.json"
        )
        assertTrue(text.contains("H1"))
    }

    @Test
    fun readAssetText_readsGzipMagic() {
        val payload = """{"ok":true}"""
        val gz = ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            bos.toByteArray()
        }
        val text = PackRepository.readAssetText(
            { ByteArrayInputStream(gz) },
            "data/glosses.json.gz"
        )
        assertEquals(payload, text)
    }

    @Test
    fun readAssetText_allMissing_listsTriedPaths() {
        try {
            PackRepository.readAssetText(
                { throw FileNotFoundException(it) },
                "data/glosses.json.gz",
                "data/glosses.json"
            )
            fail("expected IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("data/glosses.json.gz"))
            assertTrue(e.message!!.contains("data/glosses.json"))
        }
    }

    @Test
    fun sourceAssets_shipGlossesGzAndCatalog() {
        // Resolve from module working dir (Gradle test cwd = app/)
        val roots = listOf(
            java.io.File("src/main/assets/data"),
            java.io.File("../app/src/main/assets/data"),
            java.io.File("app/src/main/assets/data")
        )
        val dataDir = roots.firstOrNull { it.isDirectory }
            ?: error("assets/data not found from cwd=${java.io.File(".").absolutePath}")
        assertTrue(java.io.File(dataDir, "catalog.json").isFile)
        assertTrue(
            "expected glosses.json.gz in source assets",
            java.io.File(dataDir, "glosses.json.gz").isFile
        )
        assertTrue(java.io.File(dataDir, "books/Gen.json.gz").isFile)
    }
}
