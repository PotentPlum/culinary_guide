package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme

/** A single-line search field with a clear button. [description] is what TalkBack announces for the field. */
@Composable
fun MiseSearchField(
    query: String,
    onQuery: (String) -> Unit,
    placeholder: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = description },
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.season_search_clear))
                }
            }
        },
        shape = MaterialTheme.shapes.small,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6)
@Composable
private fun MiseSearchFieldPreview() {
    MiseTheme { MiseSearchField(query = "pomp", onQuery = {}, placeholder = "Zoeken…", description = "Groenten zoeken") }
}
