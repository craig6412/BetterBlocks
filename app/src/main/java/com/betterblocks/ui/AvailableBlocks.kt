package com.betterblocks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.betterblocks.Block
import com.betterblocks.GameUiState

@Composable
fun AvailableBlocks(
    uiState: GameUiState,
    onSelectBlock: (Block) -> Unit,
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
            if (block != null) {
                BlockPreviewCard(
                    block = block,
                    isSelected = (uiState.selectedBlock?.id == block.id),
                    onClick = { onSelectBlock(block) },
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd
                )
            }
        }
    }
}
