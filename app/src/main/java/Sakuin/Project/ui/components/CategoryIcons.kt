package Sakuin.Project.ui.components

import androidx.annotation.DrawableRes
import Sakuin.Project.R

/** Icon keys stored on categories (see DefaultCategories) mapped to drawables. */
val CategoryIconKeys: Map<String, Int> = mapOf(
    "utensils" to R.drawable.ic_utensils,
    "bus" to R.drawable.ic_bus,
    "graduation" to R.drawable.ic_graduation,
    "bag" to R.drawable.ic_bag,
    "gamepad" to R.drawable.ic_gamepad,
    "health" to R.drawable.ic_health,
    "bill" to R.drawable.ic_bill,
    "more" to R.drawable.ic_more,
    "briefcase" to R.drawable.ic_briefcase,
    "coins" to R.drawable.ic_coins,
    "gift" to R.drawable.ic_gift,
    "sparkles" to R.drawable.ic_sparkles,
    "tag" to R.drawable.ic_tag,
)

@DrawableRes
fun categoryIcon(iconKey: String?): Int = CategoryIconKeys[iconKey] ?: R.drawable.ic_tag
