package it.vfsfitvnm.vimusic.ui.styling

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.AppFont

@Immutable
data class Typography(
    val xxs: TextStyle,
    val xs: TextStyle,
    val s: TextStyle,
    val m: TextStyle,
    val l: TextStyle,
    val xxl: TextStyle,
    val font: AppFont = AppFont.Cairo,
) {
    fun copy(color: Color) = Typography(
        xxs = xxs.copy(color = color),
        xs = xs.copy(color = color),
        s = s.copy(color = color),
        m = m.copy(color = color),
        l = l.copy(color = color),
        xxl = xxl.copy(color = color),
        font = font
    )

    companion object : Saver<Typography, List<Any>> {
        override fun restore(value: List<Any>) = typographyOf(
            Color((value[0] as Long).toULong()),
            (value.getOrNull(3) as? String)?.let {
                runCatching { enumValueOf<AppFont>(it) }.getOrNull()
            } ?: if (value[1] as Boolean) AppFont.System else AppFont.Cairo,
            value[2] as Boolean
        )

        override fun SaverScope.save(value: Typography) =
            listOf(
                value.xxs.color.value.toLong(),
                value.font == AppFont.System,
                value.xxs.platformStyle?.paragraphStyle?.includeFontPadding ?: false,
                value.font.name
            )
    }
}

private val CairoFamily = FontFamily(
    Font(resId = R.font.cairo_w300, weight = FontWeight.Light),
    Font(resId = R.font.cairo_w400, weight = FontWeight.Normal),
    Font(resId = R.font.cairo_w500, weight = FontWeight.Medium),
    Font(resId = R.font.cairo_w600, weight = FontWeight.SemiBold),
    Font(resId = R.font.cairo_w700, weight = FontWeight.Bold),
)

private val TajawalFamily = FontFamily(
    Font(resId = R.font.tajawal_w400, weight = FontWeight.Normal),
    Font(resId = R.font.tajawal_w500, weight = FontWeight.Medium),
    Font(resId = R.font.tajawal_w700, weight = FontWeight.Bold),
)

private val AmiriFamily = FontFamily(
    Font(resId = R.font.amiri_w400, weight = FontWeight.Normal),
    Font(resId = R.font.amiri_w700, weight = FontWeight.Bold),
)

private val PoppinsFamily = FontFamily(
    Font(resId = R.font.poppins_w300, weight = FontWeight.Light),
    Font(resId = R.font.poppins_w400, weight = FontWeight.Normal),
    Font(resId = R.font.poppins_w500, weight = FontWeight.Medium),
    Font(resId = R.font.poppins_w600, weight = FontWeight.SemiBold),
    Font(resId = R.font.poppins_w700, weight = FontWeight.Bold),
)

fun AppFont.asFontFamily(): FontFamily = when (this) {
    AppFont.Cairo -> CairoFamily
    AppFont.Tajawal -> TajawalFamily
    AppFont.Amiri -> AmiriFamily
    AppFont.Poppins -> PoppinsFamily
    AppFont.System -> FontFamily.Default
}

fun typographyOf(color: Color, font: AppFont, applyFontPadding: Boolean): Typography {
    val textStyle = TextStyle(
        fontFamily = font.asFontFamily(),
        fontWeight = FontWeight.Normal,
        color = color,
        platformStyle = @Suppress("DEPRECATION") (PlatformTextStyle(includeFontPadding = applyFontPadding))
    )

    return Typography(
        xxs = textStyle.copy(fontSize = 12.sp),
        xs = textStyle.copy(fontSize = 14.sp),
        s = textStyle.copy(fontSize = 16.sp),
        m = textStyle.copy(fontSize = 18.sp),
        l = textStyle.copy(fontSize = 20.sp),
        xxl = textStyle.copy(fontSize = 32.sp),
        font = font
    )
}
