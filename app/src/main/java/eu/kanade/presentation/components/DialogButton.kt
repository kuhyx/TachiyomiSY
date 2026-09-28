package eu.kanade.presentation.components

/** One labelled dialog action: the label and what it does are only ever set together. */
internal data class DialogButton(val text: String, val onClick: () -> Unit)
