package com.example.smarttv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.smarttv.data.entity.Recordatorio
import com.example.smarttv.ui.components.EmptyRecordatoriosMessage
import com.example.smarttv.ui.components.RecordatoriosList
import com.example.smarttv.viewmodel.RecordatorioViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SmartTVHomeScreen(
    viewModel: RecordatorioViewModel,
    modifier: Modifier = Modifier
) {
    val recordatorios by viewModel.recordatorios.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val recordatoriosCount by viewModel.recordatoriosCount.collectAsState()

    val upcomingRecordatorios = remember(recordatorios) {
        viewModel.getUpcomingRecordatorios()
    }

    val todayRecordatorios = remember(recordatorios) {
        viewModel.getRecordatoriosForToday()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
    ) {
        if (isLoading) {
            LoadingScreen()
        } else if (recordatorios.isEmpty()) {
            EmptyRecordatoriosMessage()
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header con estadísticas
                HeaderSection(
                    totalRecordatorios = recordatoriosCount,
                    todayCount = todayRecordatorios.size,
                    upcomingCount = upcomingRecordatorios.size
                )

                // Recordatorios próximos (horizontal)
                if (upcomingRecordatorios.isNotEmpty()) {
                    UpcomingRecordatoriosSection(
                        recordatorios = upcomingRecordatorios,
                        formatDateTime = viewModel::formatDateTime
                    )
                }

                // Lista completa de recordatorios
                Text(
                    text = "Todos los Recordatorios",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                RecordatoriosList(
                    recordatorios = recordatorios,
                    formatDateTime = viewModel::formatDateTime,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    totalRecordatorios: Int,
    todayCount: Int,
    upcomingCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Smart TV Recordatorios",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = "Total",
                value = totalRecordatorios.toString(),
                color = Color(0xFF4CAF50)
            )

            StatCard(
                title = "Hoy",
                value = todayCount.toString(),
                color = Color(0xFF2196F3)
            )

            StatCard(
                title = "Próximos",
                value = upcomingCount.toString(),
                color = Color(0xFFFF9800)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(150.dp)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color.copy(alpha = 0.2f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpcomingRecordatoriosSection(
    recordatorios: List<Recordatorio>,
    formatDateTime: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Próximos Recordatorios",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recordatorios) { recordatorio ->
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF667eea),
                                        Color(0xFF764ba2)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = recordatorio.titulo,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = formatDateTime(recordatorio.fechaHora),
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando recordatorios...",
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}
