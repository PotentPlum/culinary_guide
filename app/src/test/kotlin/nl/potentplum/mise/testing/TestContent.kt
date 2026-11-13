package nl.potentplum.mise.testing

import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.ContentParser
import nl.potentplum.mise.content.CurriculumIndex
import nl.potentplum.mise.content.CurriculumParser
import nl.potentplum.mise.content.model.CourseFile

/** Test-classpath access to `data/assets` and `data/fixtures` (wired as test resources). */
object TestResources {
    fun read(name: String): String =
        requireNotNull(TestResources::class.java.classLoader!!.getResourceAsStream(name)) { "missing test resource $name" }
            .bufferedReader()
            .use { it.readText() }
}

object TestContent {
    /** Parsed once, strictly (unknown keys and versions fail), like a debug build. */
    val content: Content by lazy { ContentParser(strict = true).parse(TestResources::read) }
}

object TestCurriculum {
    private val parser = CurriculumParser(strict = true)

    /** The real curriculum index, parsed strictly once. */
    val index: CurriculumIndex by lazy { parser.index(TestResources.read(CurriculumParser.INDEX)) }

    fun course(id: String): CourseFile = parser.course(id, TestResources.read(CurriculumParser.coursePath(id)))
}
