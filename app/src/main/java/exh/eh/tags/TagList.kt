package exh.eh.tags

internal interface TagList {
    fun getTags1(): List<String>

    fun getTags2(): List<String> = emptyList()

    fun getTags3(): List<String> = emptyList()

    fun getTags(): List<List<String>> = listOf(
        getTags1(),
        getTags2(),
        getTags3(),
    )
}
