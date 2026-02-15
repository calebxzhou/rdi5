package calebxzhou.rdi.client.ui.comp

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier
) {
    // No-op on Android — native touch scrolling is sufficient
}
