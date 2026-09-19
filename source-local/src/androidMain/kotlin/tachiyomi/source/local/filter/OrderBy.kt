package tachiyomi.source.local.filter

import android.content.Context
import eu.kanade.tachiyomi.source.model.Filter
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

/** The local source's sort filter: by title or by folder modification date, either direction. */
public sealed class OrderBy(context: Context, selection: Selection) : Filter.Sort(
    context.stringResource(MR.strings.local_filter_order_by),
    arrayOf(context.stringResource(MR.strings.title), context.stringResource(MR.strings.date)),
    selection,
) {
    /** Title ascending: the "popular" listing. */
    public class Popular(context: Context) : OrderBy(context, Selection(0, true))

    /** Date descending: the "latest" listing. */
    public class Latest(context: Context) : OrderBy(context, Selection(1, false))
}
