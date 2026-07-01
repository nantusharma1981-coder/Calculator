package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calculator.CalculatorViewModel
import com.example.calculator.CalculatorViewModelFactory
import com.example.data.CalculationHistory
import com.example.data.CalculationRepository
import com.example.data.CalculatorDatabase
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val database by lazy { CalculatorDatabase.getDatabase(applicationContext) }
    private val repository by lazy { CalculationRepository(database.calculationDao()) }
    private val viewModel: CalculatorViewModel by viewModels {
        CalculatorViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0C0E))
                ) { innerPadding ->
                    CalculatorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val expression by viewModel.expression.collectAsStateWithLifecycle()
    val previewResult by viewModel.previewResult.collectAsStateWithLifecycle()
    val useRadians by viewModel.useRadians.collectAsStateWithLifecycle()
    val showHistory by viewModel.showHistory.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. DISPLAY SCREEN
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .padding(24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Expression
                    Text(
                        text = expression.ifEmpty { "0" },
                        fontSize = if (expression.length > 15) 32.sp else 44.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expression_display")
                    )

                    // Preview Result or Error
                    if (previewResult.isNotEmpty()) {
                        Text(
                            text = previewResult,
                            fontSize = if (previewResult.startsWith("Error")) 18.sp else 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (previewResult.startsWith("Error")) Color(0xFFFF453A) else Color(0xFF30D5C8),
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preview_display")
                        )
                    } else if (expression.isEmpty()) {
                        // Empty indicator helper
                        Text(
                            text = "Scientific Calculator",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF5A5A65),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Divider separating display from keys
            HorizontalDivider(
                color = Color(0xFF1F1F24),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 2. MODES & SETTINGS ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DEG/RAD Toggle Badge
                Button(
                    onClick = { viewModel.toggleRadians() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1C1C24),
                        contentColor = Color(0xFF30D5C8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("rad_deg_toggle")
                ) {
                    Text(
                        text = if (useRadians) "RAD" else "DEG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // History Toggle Button
                Button(
                    onClick = { viewModel.toggleHistory() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showHistory) Color(0xFF30D5C8) else Color(0xFF1C1C24),
                        contentColor = if (showHistory) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("history_toggle")
                ) {
                    Text(
                        text = "HISTORY (${historyList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // 3. KEYBOARD AREA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3.0f)
                    .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // -- SCIENTIFIC SECTION (5 columns, compact sizing) --
                val sciKeys = listOf(
                    listOf("sin", "cos", "tan", "log", "ln"),
                    listOf("asin", "acos", "atan", "sqrt", "^"),
                    listOf("π", "e", "!", "(", ")")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sciKeys.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { key ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF181820))
                                        .clickable { viewModel.onCharacterInput(key) }
                                        .testTag("btn_$key")
                                ) {
                                    Text(
                                        text = when(key) {
                                            "sqrt" -> "√"
                                            "asin" -> "sin⁻¹"
                                            "acos" -> "cos⁻¹"
                                            "atan" -> "tan⁻¹"
                                            else -> key
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF30D5C8)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // -- BASIC SECTION (4 columns, classic sizing) --
                val basicRows = listOf(
                    listOf("C", "⌫", "%", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "=")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    basicRows.forEachIndexed { rowIndex, row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { key ->
                                val isOperator = key in listOf("÷", "×", "-", "+")
                                val isEqual = key == "="
                                val isUtility = key in listOf("C", "⌫", "%")
                                
                                val weight = if (key == "0") 2f else 1f

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(weight)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            when {
                                                isEqual -> Color(0xFF30D5C8)
                                                isOperator -> Color(0xFF2C2C35)
                                                isUtility -> Color(0xFF1C1C24)
                                                else -> Color(0xFF25252B)
                                            }
                                        )
                                        .clickable {
                                            when (key) {
                                                "C" -> viewModel.onClear()
                                                "⌫" -> viewModel.onBackspace()
                                                "=" -> viewModel.onEvaluate()
                                                else -> viewModel.onCharacterInput(key)
                                            }
                                        }
                                        .testTag("btn_$key")
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = if (isEqual || isOperator) 24.sp else 20.sp,
                                        fontWeight = if (isEqual || isOperator) FontWeight.Bold else FontWeight.SemiBold,
                                        color = when {
                                            isEqual -> Color.Black
                                            isOperator -> Color(0xFFFF9F0A)
                                            isUtility -> if (key == "C") Color(0xFFFF453A) else Color.White
                                            else -> Color.White
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. HISTORIC SLIDING DRAWER / OVERLAY PANEL
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF141419))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleHistory() },
                                modifier = Modifier.testTag("close_history")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Close History",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Calculation History",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (historyList.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearAllHistory() },
                                modifier = Modifier.testTag("clear_all_history")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear All History",
                                    tint = Color(0xFFFF453A)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // History list
                    if (historyList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "No History",
                                    tint = Color(0xFF42424F),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No history recorded yet",
                                    fontSize = 14.sp,
                                    color = Color(0xFF5A5A65)
                                )
                            }
                        }
                    } else {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(historyList, key = { it.id }) { entry ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF1E1E24)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("history_item_${entry.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Left side: Clickable formula restoration
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        viewModel.selectHistoryEntry(entry, true)
                                                        viewModel.toggleHistory()
                                                    }
                                            ) {
                                                Text(
                                                    text = entry.expression,
                                                    fontSize = 16.sp,
                                                    color = Color(0xFFD4D4D8)
                                                )
                                                Text(
                                                    text = "= ${entry.result}",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF30D5C8)
                                                )
                                            }

                                            // Right side: Quick actions
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Append Result button
                                                Button(
                                                    onClick = {
                                                        viewModel.selectHistoryEntry(entry, false)
                                                        viewModel.toggleHistory()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF2E2E38),
                                                        contentColor = Color.White
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("USE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                // Delete individual entry
                                                IconButton(
                                                    onClick = { viewModel.deleteHistoryItem(entry.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Item",
                                                        tint = Color(0xFF8E8E93),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
