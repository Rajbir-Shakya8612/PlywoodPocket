package com.plywoodpocket.crm.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plywoodpocket.crm.ui.theme.Orange
import com.plywoodpocket.crm.ui.theme.OrangeLight
import com.plywoodpocket.crm.ui.theme.White
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfiniteCardView() {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Sample data - replace with actual data from API
    val cards = remember { mutableStateListOf<CardData>() }
    
    // Simulate loading more data when reaching the end
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= cards.size - 2) {
                    // Load more data here when API is integrated
                    // For now, just add sample data
                    cards.addAll(generateSampleCards())
                }
            }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(cards) { card ->
            CardItem(card)
        }
    }
}

@Composable
fun CardItem(card: CardData) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(listOf(Orange, OrangeLight))
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = card.title,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = card.subtitle,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = card.description,
                    color = White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

data class CardData(
    val title: String,
    val subtitle: String,
    val description: String
)

private fun generateSampleCards(): List<CardData> {
    return List(5) { index ->
        CardData(
            title = "Card ${index + 1}",
            subtitle = "Sample Content ${index + 1}",
            description = "This is a sample card that will be replaced with actual content from the API"
        )
    }
} 