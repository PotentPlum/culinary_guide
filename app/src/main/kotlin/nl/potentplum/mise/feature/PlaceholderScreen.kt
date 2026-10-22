package nl.potentplum.mise.feature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem

/** A screen whose feature is not built yet (P1 in `todo.md`): its top bar and a short note, plus an optional [header]. */
@Composable
fun PlaceholderScreen(
    title: String,
    onOverflowItem: (OverflowItem) -> Unit,
    onBack: (() -> Unit)? = null,
    header: @Composable () -> Unit = {},
) {
    Scaffold(topBar = { MiseTopAppBar(title = title, onOverflowItem = onOverflowItem, onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            header()
            EmptyState(title = stringResource(R.string.placeholder_under_construction), icon = Icons.Outlined.Construction)
        }
    }
}
