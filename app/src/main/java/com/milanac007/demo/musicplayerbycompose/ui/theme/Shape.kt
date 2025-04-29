package com.milanac007.demo.musicplayerbycompose.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
定义形状
Compose 提供带有扩展参数的 Shapes 类来实现新的 M3 形状。M3 形状比例与字体比例类似，能够在整个界面中呈现丰富多样的形状。

形状比例中包含不同大小的形状：

特小
小
中
大
特大
默认情况下，每个形状都有一个可以被覆盖的默认值。对于您的应用，您可以使用中等形状来修改列表项，不过您也可以声明其他形状。
 */

val shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)
//您已经定义了 shapes，请按照与颜色和排版相同的方式将其传递给 M3 MaterialTheme：