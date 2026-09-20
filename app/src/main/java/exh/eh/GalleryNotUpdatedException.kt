package exh.eh

internal class GalleryNotUpdatedException(val network: Boolean, cause: Throwable) : RuntimeException(cause)
