package com.example.pkco.ui.notes

/** Length limits enforced while typing; extra characters are cut off. */
internal object InputLimits {
    const val TITLE = 200
    const val BODY = 20_000
    const val TAG = 40
    const val CATEGORY_NAME = 60
    const val TAGS_PER_NOTE = 20

    /** The body counter shows only above this length (90% of [BODY]). */
    const val BODY_COUNTER_FROM = BODY * 9 / 10
}
