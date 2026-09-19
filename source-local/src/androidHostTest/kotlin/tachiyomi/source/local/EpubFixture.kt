package tachiyomi.source.local

/** A three-entry EPUB: the container, the package document and one page holding one image. */
internal object EpubFixture {
    const val CONTAINER_PATH: String = "META-INF/container.xml"
    const val OPF_PATH: String = "OEBPS/content.opf"
    const val PAGE_PATH: String = "OEBPS/page1.xhtml"
    const val IMAGE_PATH: String = "OEBPS/images/001.png"

    /** Milliseconds since the epoch of the date in [FULL_METADATA]. */
    const val FULL_METADATA_DATE: Long = 1_577_934_245_000L

    /** A page with a single image. */
    const val PAGE_WITH_IMAGE: String = """<html><body><img src="images/001.png"/></body></html>"""

    /** Every `dc:` element the metadata reader looks at, plus a date in the parseable format. */
    val FULL_METADATA: String = """
        <dc:title>Chapter One</dc:title>
        <dc:creator>Author A</dc:creator>
        <dc:publisher>Pub P</dc:publisher>
        <dc:description>Desc D</dc:description>
        <dc:date>2020-01-02T03:04:05+0000</dc:date>
    """.trimIndent()

    /** The `META-INF/container.xml` pointing at [OPF_PATH]. */
    val container: String = """
        <?xml version="1.0"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles>
            <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
          </rootfiles>
        </container>
    """.trimIndent()

    /** The package document holding [metadata], with one xhtml page in the spine. */
    fun opf(metadata: String): String = """
        <?xml version="1.0"?>
        <package xmlns="http://www.idpf.org/2007/opf" xmlns:dc="http://purl.org/dc/elements/1.1/" version="3.0">
          <metadata>
            $metadata
          </metadata>
          <manifest>
            <item id="p1" href="page1.xhtml" media-type="application/xhtml+xml"/>
            <item id="img1" href="images/001.png" media-type="image/png"/>
          </manifest>
          <spine>
            <itemref idref="p1"/>
          </spine>
        </package>
    """.trimIndent()

    /** The archive entries of an EPUB whose package document holds [metadata] and whose page is [page]. */
    fun entries(metadata: String = FULL_METADATA, page: String = PAGE_WITH_IMAGE): Map<String, ByteArray> = mapOf(
        CONTAINER_PATH to container.toByteArray(),
        OPF_PATH to opf(metadata).toByteArray(),
        PAGE_PATH to page.toByteArray(),
        IMAGE_PATH to PNG_HEADER,
    )
}
