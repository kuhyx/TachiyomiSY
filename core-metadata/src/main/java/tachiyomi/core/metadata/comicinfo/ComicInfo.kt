package tachiyomi.core.metadata.comicinfo

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

/**
 * A ComicInfo.xml document (https://anansi-project.github.io/docs/comicinfo/schemas/v2.0), one
 * optional element per property; the last three are Tachiyomi, Mihon and SY extensions.
 *
 * @property title the `<Title>` element.
 * @property series the `<Series>` element.
 * @property number the `<Number>` element.
 * @property summary the `<Summary>` element.
 * @property writer the `<Writer>` element.
 * @property penciller the `<Penciller>` element.
 * @property inker the `<Inker>` element.
 * @property colorist the `<Colorist>` element.
 * @property letterer the `<Letterer>` element.
 * @property coverArtist the `<CoverArtist>` element.
 * @property translator the `<Translator>` element.
 * @property genre the `<Genre>` element.
 * @property tags the `<Tags>` element.
 * @property web the `<Web>` element.
 * @property publishingStatus the `<PublishingStatusTachiyomi>` element.
 * @property categories the `<Categories>` element.
 * @property source the `<SourceMihon>` element.
 * @property padding the `<PaddingTachiyomiSY>` element (SY: reader padding hint).
 */
@Serializable
@XmlSerialName("ComicInfo", "", "")
public data class ComicInfo(
    val title: Title?,
    val series: Series?,
    val number: Number?,
    val summary: Summary?,
    val writer: Writer?,
    val penciller: Penciller?,
    val inker: Inker?,
    val colorist: Colorist?,
    val letterer: Letterer?,
    val coverArtist: CoverArtist?,
    val translator: Translator?,
    val genre: Genre?,
    val tags: Tags?,
    val web: Web?,
    val publishingStatus: PublishingStatusTachiyomi?,
    val categories: CategoriesTachiyomi?,
    val source: SourceMihon?,
    // SY -->
    val padding: PaddingTachiyomiSY?,
    // SY <--
) {
    /** The `xmlns:xsd` attribute every document carries. */
    @XmlElement(false)
    @XmlSerialName("xmlns:xsd", "", "")
    public val xmlSchema: String = "http://www.w3.org/2001/XMLSchema"

    /** The `xmlns:xsi` attribute every document carries. */
    @XmlElement(false)
    @XmlSerialName("xmlns:xsi", "", "")
    public val xmlSchemaInstance: String = "http://www.w3.org/2001/XMLSchema-instance"

    /**
     * The `Title` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Title", "", "")
    public data class Title(@XmlValue(true) val value: String = "")

    /**
     * The `Series` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Series", "", "")
    public data class Series(@XmlValue(true) val value: String = "")

    /**
     * The `Number` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Number", "", "")
    public data class Number(@XmlValue(true) val value: String = "")

    /**
     * The `Summary` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Summary", "", "")
    public data class Summary(@XmlValue(true) val value: String = "")

    /**
     * The `Writer` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Writer", "", "")
    public data class Writer(@XmlValue(true) val value: String = "")

    /**
     * The `Penciller` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Penciller", "", "")
    public data class Penciller(@XmlValue(true) val value: String = "")

    /**
     * The `Inker` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Inker", "", "")
    public data class Inker(@XmlValue(true) val value: String = "")

    /**
     * The `Colorist` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Colorist", "", "")
    public data class Colorist(@XmlValue(true) val value: String = "")

    /**
     * The `Letterer` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Letterer", "", "")
    public data class Letterer(@XmlValue(true) val value: String = "")

    /**
     * The `CoverArtist` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("CoverArtist", "", "")
    public data class CoverArtist(@XmlValue(true) val value: String = "")

    /**
     * The `Translator` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Translator", "", "")
    public data class Translator(@XmlValue(true) val value: String = "")

    /**
     * The `Genre` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Genre", "", "")
    public data class Genre(@XmlValue(true) val value: String = "")

    /**
     * The `Tags` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Tags", "", "")
    public data class Tags(@XmlValue(true) val value: String = "")

    /**
     * The `Web` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Web", "", "")
    public data class Web(@XmlValue(true) val value: String = "")

    // The spec doesn't have a good field for this

    /**
     * The `PublishingStatusTachiyomi` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("PublishingStatusTachiyomi", "http://www.w3.org/2001/XMLSchema", "ty")
    public data class PublishingStatusTachiyomi(@XmlValue(true) val value: String = "")

    /**
     * The `Categories` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("Categories", "http://www.w3.org/2001/XMLSchema", "ty")
    public data class CategoriesTachiyomi(@XmlValue(true) val value: String = "")

    /**
     * The `SourceMihon` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("SourceMihon", "http://www.w3.org/2001/XMLSchema", "mh")
    public data class SourceMihon(@XmlValue(true) val value: String = "")

    // SY -->

    /**
     * The `PaddingTachiyomiSY` element.
     *
     * @property value the element text.
     */
    @Serializable
    @XmlSerialName("PaddingTachiyomiSY", "http://www.w3.org/2001/XMLSchema", "tysy")
    public data class PaddingTachiyomiSY(@XmlValue(true) val value: String = "")
    // SY <--
}
