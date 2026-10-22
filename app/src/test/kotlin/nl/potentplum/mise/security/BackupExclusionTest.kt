package nl.potentplum.mise.security

import nl.potentplum.mise.data.settings.DataStoreModule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The secrets file must be excluded from cloud backup, device transfer and API 30 Auto Backup (spec §9). */
class BackupExclusionTest {
    private val secretsPath = "datastore/${DataStoreModule.SECRETS_FILE}.preferences_pb"

    private fun excludes(
        file: String,
        section: String? = null,
    ): List<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/xml/$file"))
        val root = section?.let { doc.getElementsByTagName(it).item(0) as Element } ?: doc.documentElement
        val nodes = root.getElementsByTagName("exclude")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .filter { it.getAttribute("domain") == "file" }
            .map { it.getAttribute("path") }
    }

    @Test
    fun `cloud backup excludes the secrets file`() =
        assertEquals(listOf(secretsPath), excludes("data_extraction_rules.xml", "cloud-backup"))

    @Test
    fun `device transfer excludes the secrets file`() =
        assertEquals(listOf(secretsPath), excludes("data_extraction_rules.xml", "device-transfer"))

    @Test
    fun `auto backup on android 11 excludes the secrets file`() = assertEquals(listOf(secretsPath), excludes("full_backup_content.xml"))
}
