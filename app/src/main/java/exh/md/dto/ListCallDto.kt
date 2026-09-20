package exh.md.dto

internal interface ListCallDto<T> {
    val limit: Int
    val offset: Int
    val total: Int
    val data: List<T>
}
