package com.example.olahragakuy.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)
private val base = TextStyle(fontFamily = Poppins, letterSpacing = 0.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
val Typography = Typography(
    headlineLarge = base.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 48.sp),
    titleLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 30.sp),
    labelLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = base.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = base.copy(fontWeight = FontWeight.Light, fontSize = 11.sp, lineHeight = 17.sp),
)
