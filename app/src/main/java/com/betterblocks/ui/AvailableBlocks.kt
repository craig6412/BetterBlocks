package com.betterblocks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.betterblocks.Block
import com.betterblocks.GameUiState
import com.betterblocks.InteractionType
import android.util.Log

@Composable
fun AvailableBlocks(
    uiState: GameUiState,
    onBlockInteraction: (Block, InteractionType) -> Unit,
    onDragStart: (Block, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        uiState.availableBlocks.forEach { block ->
            // Use key() to ensure proper recomposition when blocks change
            key(block.id) {
                BlockPreviewCard(
                    block = block,
                    isSelected = (uiState.selectedBlock?.id == block.id),
                    onClick = {
                        // TAP interaction: toggle selection
                        onBlockInteraction(block, InteractionType.TAP)
                    },
                    // DRAG_START interaction: always select the block being dragged
                    onDragStart = { _, startPosition ->
                        val latest = uiState.availableBlocks.firstOrNull { it.id == block.id } ?: block
                        onBlockInteraction(latest, InteractionType.DRAG_START)
                        onDragStart(latest, startPosition)
                    },
                    onDrag = onDrag,
                    onDragEnd = onDragEnd
                )
            }
        }
    }
}
