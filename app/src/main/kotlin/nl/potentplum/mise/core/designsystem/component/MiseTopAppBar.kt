package nl.potentplum.mise.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme

/** The overflow menu every top app bar carries (spec §6.1). */
enum class OverflowItem { Techniques, Settings, About }

/** A top app bar with an optional back arrow, screen [actions], and the ⋮ menu: Technieken, Instellingen, Over Mise. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiseTopAppBar(
    title: String,
    onOverflowItem: (OverflowItem) -> Unit,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
            }
        },
        actions = {
            actions()
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.menu_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                OverflowItem.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(stringResource(item.labelRes)) },
                        onClick = {
                            menuOpen = false
                            onOverflowItem(item)
                        },
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    )
}

private val OverflowItem.labelRes: Int
    get() =
        when (this) {
            OverflowItem.Techniques -> R.string.nav_techniques
            OverflowItem.Settings -> R.string.menu_settings
            OverflowItem.About -> R.string.menu_about
        }

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6)
@Composable
private fun MiseTopAppBarPreview() {
    MiseTheme { MiseTopAppBar(title = "Instellingen", onOverflowItem = {}, onBack = {}) }
}
