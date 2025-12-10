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
import com.betterblocks.BlockPreviewCard
import com.betterblocks.GameUiState
import com.betterblocks.InteractionType

@Composable
fun AvailableBlocks(
    uiState: GameUiState,
    onBlockInteraction: (Block, InteractionType) -> Unit,
    onDragStart: (Block, Offset) -> Unit,  // ✅ Block + Offset
    onDrag: (Offset) -> Unit,              // ✅ Just Offset
    onDragEnd: () -> Unit
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        uiState.availableBlocks.forEach { block ->
            key(block.id) {
                BlockPreviewCard(
                    block = block,
                    isSelected = (uiState.selectedBlock?.id == block.id),
                    onClick = {
                        onBlockInteraction(block, InteractionType.TAP)
                    },
                    onDragStart = { fingerRootPos ->
                        onDragStart(block, fingerRootPos)  // Pass Block + Offset
                    },
                    onDrag = { currentFingerPos ->
                        onDrag(currentFingerPos)  // Pass current position
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                )
            }
        }
    }
}