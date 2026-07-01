package com.example.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalculationHistory
import com.example.data.CalculationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.text.DecimalFormat

class CalculatorViewModel(private val repository: CalculationRepository) : ViewModel() {

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _previewResult = MutableStateFlow("")
    val previewResult: StateFlow<String> = _previewResult.asStateFlow()

    private val _useRadians = MutableStateFlow(true)
    val useRadians: StateFlow<Boolean> = _useRadians.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    val historyList: StateFlow<List<CalculationHistory>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onCharacterInput(char: String) {
        // Prevent multiple consecutive operators or decimals
        val current = _expression.value
        
        // If there's an error displayed in preview, typing something should clear the preview or we reset
        if (_previewResult.value.startsWith("Error")) {
            _previewResult.value = ""
        }

        // Smart append based on input
        when (char) {
            "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt" -> {
                _expression.value = current + "$char("
            }
            "π", "e" -> {
                _expression.value = current + char
            }
            else -> {
                _expression.value = current + char
            }
        }
        updatePreview()
    }

    fun onClear() {
        _expression.value = ""
        _previewResult.value = ""
    }

    fun onBackspace() {
        val current = _expression.value
        if (current.isNotEmpty()) {
            val prefixes = listOf("asin(", "acos(", "atan(", "sqrt(", "sin(", "cos(", "tan(", "log(", "ln(")
            var deleted = false
            for (prefix in prefixes) {
                if (current.endsWith(prefix)) {
                    _expression.value = current.dropLast(prefix.length)
                    deleted = true
                    break
                }
            }
            if (!deleted) {
                _expression.value = current.dropLast(1)
            }
            updatePreview()
        }
    }

    fun toggleRadians() {
        _useRadians.value = !_useRadians.value
        updatePreview()
    }

    fun toggleHistory() {
        _showHistory.value = !_showHistory.value
    }

    fun selectHistoryEntry(entry: CalculationHistory, restoreExpression: Boolean) {
        if (restoreExpression) {
            _expression.value = entry.expression
        } else {
            _expression.value = _expression.value + entry.result
        }
        updatePreview()
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun onEvaluate() {
        val expr = _expression.value
        if (expr.isEmpty()) return

        val cleanExpr = prepareExpressionForParsing(expr)
        try {
            val evalResult = MathParser(_useRadians.value).evaluate(cleanExpr)
            val formattedResult = formatResult(evalResult)

            viewModelScope.launch {
                repository.insert(
                    CalculationHistory(
                        expression = expr,
                        result = formattedResult
                    )
                )
            }
            _expression.value = formattedResult
            _previewResult.value = ""
        } catch (e: Exception) {
            _previewResult.value = "Error: ${e.message ?: "Syntax Error"}"
        }
    }

    private fun updatePreview() {
        val expr = _expression.value
        if (expr.isEmpty()) {
            _previewResult.value = ""
            return
        }
        val cleanExpr = prepareExpressionForParsing(expr)
        try {
            val evalResult = MathParser(_useRadians.value).evaluate(cleanExpr)
            if (evalResult.isNaN() || evalResult.isInfinite()) {
                _previewResult.value = ""
            } else {
                _previewResult.value = formatResult(evalResult)
            }
        } catch (e: Exception) {
            _previewResult.value = ""
        }
    }

    private fun prepareExpressionForParsing(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite()) return "∞"
        if (value.isNaN()) return "Error"

        // Check if it's an integer
        if (value == value.toLong().toDouble()) {
            return value.toLong().toString()
        }

        val df = DecimalFormat("#.##########")
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(value)
    }
}
