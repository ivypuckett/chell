package dev.chell.launcher

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable

/**
 * The icon a folder cell shows: its first few members tiled inside a rounded
 * plate, which is what tells a folder apart from an app at a glance.
 *
 * A folder has no name, so this is the only thing distinguishing it. Four is
 * the most that stays legible at cell size; a fuller folder simply shows the
 * first four.
 */
object FolderIcon {

    private const val TILES = 4

    fun of(context: Context, icons: List<Drawable>): Drawable {
        val plate = context.getDrawable(R.drawable.folder_plate)
        val shown = icons.take(TILES)
        if (shown.isEmpty()) return plate ?: LayerDrawable(emptyArray())

        val size = context.resources.getDimensionPixelSize(R.dimen.folder_icon_size)
        val layers = LayerDrawable((listOfNotNull(plate) + shown).toTypedArray())

        val gap = size / 10
        val tile = (size - 3 * gap) / 2
        val offset = if (plate == null) 0 else 1

        shown.forEachIndexed { i, _ ->
            val left = gap + (i % 2) * (tile + gap)
            val top = gap + (i / 2) * (tile + gap)
            layers.setLayerSize(i + offset, tile, tile)
            layers.setLayerInset(i + offset, left, top, size - left - tile, size - top - tile)
        }
        return layers
    }
}
