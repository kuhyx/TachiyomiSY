package exh.metadata.metadata.base

import kotlinx.serialization.Serializable

@Serializable
public data class RaisedTitle(
    val title: String,
    val type: Int = 0,
)
