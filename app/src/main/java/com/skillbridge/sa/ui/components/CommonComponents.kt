package com.skillbridge.sa.ui.components

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.skillbridge.sa.R

@Composable
fun SkillBridgeHeaderBanner(subtitle: String? = null) {
    AndroidView<View>(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_header_banner, null, false).apply {
                val tvSubtitle = findViewById<TextView>(R.id.tvHeaderSubtitle)
                if (subtitle != null) {
                    tvSubtitle.text = subtitle
                    tvSubtitle.visibility = View.VISIBLE
                } else {
                    tvSubtitle.visibility = View.GONE
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SbPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    AndroidView<View>(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_primary_button, null, false).apply {
                val btn = findViewById<Button>(R.id.btnPrimary)
                btn.text = text
                btn.isEnabled = enabled
                btn.setOnClickListener { onClick() }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun SbTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    singleLine: Boolean = true
) {
    AndroidView<View>(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_text_field, null, false).apply {
                val til = findViewById<TextInputLayout>(R.id.textInputLayout)
                val et = findViewById<TextInputEditText>(R.id.textInputEditText)
                til.hint = label
                et.setText(value)
                et.isSingleLine = singleLine
                if (isPassword) {
                    et.transformationMethod = PasswordTransformationMethod.getInstance()
                    til.isPasswordVisibilityToggleEnabled = true
                }
                et.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        onValueChange(s?.toString() ?: "")
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun RoleBadge(label: String, background: Color = Color(0xFF0E7A4C)) {
    AndroidView<View>(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_role_badge, null, false).apply {
                val tv = findViewById<TextView>(R.id.tvRoleBadge)
                tv.text = label
                tv.backgroundTintList = ColorStateList.valueOf(background.toArgb())
            }
        }
    )
}

@Composable
fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    AndroidView<View>(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_stat_chip, null, false).apply {
                findViewById<TextView>(R.id.tvStatValue).text = value
                findViewById<TextView>(R.id.tvStatLabel).text = label
            }
        },
        modifier = modifier
    )
}

@Composable
fun QuickActionCard(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AndroidView<View>(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_quick_action_card, null, false).apply {
                findViewById<TextView>(R.id.tvActionTitle).text = title
                findViewById<TextView>(R.id.tvActionSubtitle).text = subtitle
                setOnClickListener { onClick() }
            }
        },
        modifier = modifier
    )
}

@Composable
fun GoldBanner(text: String) {
    AndroidView<View>(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_gold_banner, null, false).apply {
                findViewById<TextView>(R.id.tvGoldBannerText).text = text
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    AndroidView<View>(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_section_header, null, false).apply {
                findViewById<TextView>(R.id.tvSectionTitle).text = title
                val tvSeeAll = findViewById<TextView>(R.id.tvSeeAll)
                if (onSeeAll != null) {
                    tvSeeAll.visibility = View.VISIBLE
                    tvSeeAll.setOnClickListener { onSeeAll() }
                } else {
                    tvSeeAll.visibility = View.GONE
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SbBottomNavBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    AndroidView<View>(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_bottom_nav, null, false).apply {
                val nav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                nav.setOnItemSelectedListener { item ->
                    val route = when (item.itemId) {
                        R.id.home -> "home"
                        R.id.explore -> "explore"
                        R.id.messages -> "messages"
                        R.id.notifications -> "notifications"
                        R.id.profile -> "profile"
                        else -> "home"
                    }
                    onNavigate(route)
                    true
                }
            }
        },
        update = { view ->
            val nav = view.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            val itemId = when (currentRoute) {
                "home" -> R.id.home
                "explore" -> R.id.explore
                "messages" -> R.id.messages
                "notifications" -> R.id.notifications
                "profile" -> R.id.profile
                else -> R.id.home
            }
            if (nav.selectedItemId != itemId) {
                nav.selectedItemId = itemId
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

val screenPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
