package com.class1c.thyme.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val fast_forward: ImageVector
    get() {
        if (_fast_forward != null) {
            return _fast_forward!!
        }
        _fast_forward =
            ImageVector.Builder(
                name = "fast_forward",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(2.5f, 18f)
                        verticalLineTo(6f)
                        lineToRelative(9f, 6f)
                        lineToRelative(-9f, 6f)
                        close()
                        moveToRelative(10f, 0f)
                        verticalLineTo(6f)
                        lineToRelative(9f, 6f)
                        lineToRelative(-9f, 6f)
                        close()
                        moveToRelative(-8f, -6f)
                        close()
                        moveToRelative(10f, 0f)
                        close()
                        moveToRelative(-10f, 2.25f)
                        lineTo(7.9f, 12f)
                        lineTo(4.5f, 9.75f)
                        verticalLineToRelative(4.5f)
                        close()
                        moveToRelative(10f, 0f)
                        lineTo(17.9f, 12f)
                        lineTo(14.5f, 9.75f)
                        verticalLineToRelative(4.5f)
                        close()
                    }
                }
                .build()
        return _fast_forward!!
    }

private var _fast_forward: ImageVector? = null
