"""Tests for proto_numbers.py."""

from proto_numbers import rewrite, snake

SOURCE = """package x

import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class BackupThing(
    @ProtoNumber(1) var source: Long,
    @ProtoNumber(103) var viewerFlags: Int? = null,
) {
    @Serializable
    class Inner(@ProtoNumber(2) val id: Long)
}
"""


def test_snake() -> None:
    assert snake("BackupMergedMangaReference") == "BACKUP_MERGED_MANGA_REFERENCE"
    assert snake("viewerFlags") == "VIEWER_FLAGS"


def test_rewrite_names_every_tag_per_class() -> None:
    text, count = rewrite(SOURCE)
    assert count == 3
    assert "private const val BACKUP_THING_SOURCE = 1\n" in text
    assert "private const val BACKUP_THING_VIEWER_FLAGS = 103\n" in text
    assert "private const val INNER_ID = 2\n" in text
    assert "@ProtoNumber(BACKUP_THING_VIEWER_FLAGS) var viewerFlags: Int? = null," in text
    assert text.index("private const val") < text.index("@Serializable")
    assert rewrite(text) == (text, 0)
