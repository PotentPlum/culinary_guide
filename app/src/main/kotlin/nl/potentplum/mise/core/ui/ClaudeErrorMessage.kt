package nl.potentplum.mise.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.claude.ClaudeError

/** Dutch copy for every Claude failure (spec §8.1); raw messages are never shown (spec §9). */
@Composable
fun claudeErrorMessage(error: ClaudeError): String =
    when (error) {
        ClaudeError.NoKey -> {
            stringResource(R.string.claude_error_no_key)
        }

        ClaudeError.InvalidKey -> {
            stringResource(R.string.claude_error_invalid_key)
        }

        is ClaudeError.Refused -> {
            error.explanation?.let { stringResource(R.string.claude_error_refused_explained, it) }
                ?: stringResource(R.string.claude_error_refused)
        }

        ClaudeError.Truncated -> {
            stringResource(R.string.claude_error_truncated)
        }

        ClaudeError.Network -> {
            stringResource(R.string.claude_error_network)
        }

        ClaudeError.RateLimited -> {
            stringResource(R.string.claude_error_rate_limited)
        }

        is ClaudeError.Http -> {
            stringResource(R.string.claude_error_http, error.code)
        }

        ClaudeError.InvalidResponse -> {
            stringResource(R.string.claude_error_invalid_response)
        }
    }
