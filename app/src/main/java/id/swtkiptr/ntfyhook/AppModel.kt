
package id.swtkiptr.ntfyhook

import android.graphics.drawable.Drawable

data class AppModel(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable,
    var isSelected: Boolean = false
)