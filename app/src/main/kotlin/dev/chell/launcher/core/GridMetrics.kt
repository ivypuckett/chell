package dev.chell.launcher.core

/**
 * How many cells of a given size fit into an available area.
 *
 * Kept here rather than in the Android layer so the arithmetic is unit
 * testable; callers supply already-measured pixel dimensions.
 */
data class GridMetrics(val columns: Int, val rows: Int) {

    init {
        require(columns > 0) { "columns must be positive" }
        require(rows > 0) { "rows must be positive" }
    }

    val pageSize: Int get() = columns * rows

    companion object {
        /**
         * Fits as many whole [cellWidth] x [cellHeight] cells as possible into
         * [availableWidth] x [availableHeight].
         *
         * Always yields at least a 1x1 grid: a page showing one cropped icon is
         * more useful than a page showing nothing, and [AppDrawer] requires a
         * positive page size.
         */
        fun fit(
            availableWidth: Int,
            availableHeight: Int,
            cellWidth: Int,
            cellHeight: Int,
        ): GridMetrics {
            require(cellWidth > 0) { "cellWidth must be positive" }
            require(cellHeight > 0) { "cellHeight must be positive" }
            return GridMetrics(
                columns = (availableWidth / cellWidth).coerceAtLeast(1),
                rows = (availableHeight / cellHeight).coerceAtLeast(1),
            )
        }
    }
}
