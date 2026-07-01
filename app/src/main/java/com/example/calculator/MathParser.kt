package com.example.calculator

import kotlin.math.*

class MathParser(private val useRadians: Boolean = true) {

    fun evaluate(expression: String): Double {
        if (expression.trim().isEmpty()) return 0.0
        return object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expression.length) expression[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                while (ch == ' '.code) nextChar()
                if (pos < expression.length) throw IllegalArgumentException("Unexpected character: " + ch.toChar())
                return x
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)` | number | functionName factor | factor `^` factor

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code) || eat('×'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code) || eat('÷'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor // division
                    } else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code) // try to consume closing bracket
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    val numStr = expression.substring(startPos, this.pos)
                    x = numStr.toDoubleOrNull() ?: 0.0
                } else if (ch >= 'a'.code && ch <= 'z'.code || ch == 'π'.code || ch == 'e'.code) { // functions or constants
                    while ((ch >= 'a'.code && ch <= 'z'.code) || ch == 'π'.code || ch == 'e'.code) nextChar()
                    val func = expression.substring(startPos, this.pos)
                    if (func == "π" || func == "pi") {
                        x = PI
                    } else if (func == "e") {
                        x = E
                    } else {
                        // Function with parameter
                        val arg = parseFactor()
                        x = when (func) {
                            "sin" -> if (useRadians) sin(arg) else sin(Math.toRadians(arg))
                            "cos" -> if (useRadians) cos(arg) else cos(Math.toRadians(arg))
                            "tan" -> if (useRadians) tan(arg) else tan(Math.toRadians(arg))
                            "asin" -> if (useRadians) asin(arg) else Math.toDegrees(asin(arg))
                            "acos" -> if (useRadians) acos(arg) else Math.toDegrees(acos(arg))
                            "atan" -> if (useRadians) atan(arg) else Math.toDegrees(atan(arg))
                            "sqrt" -> {
                                if (arg < 0) throw ArithmeticException("Square root of negative number")
                                sqrt(arg)
                            }
                            "log" -> log10(arg)
                            "ln" -> ln(arg)
                            else -> throw IllegalArgumentException("Unknown function: $func")
                        }
                    }
                } else {
                    throw IllegalArgumentException("Unexpected character: " + ch.toChar())
                }

                if (eat('^'.code)) {
                    x = x.pow(parseFactor()) // exponentiation
                }

                // Check for factorial or percentage postfix operators
                while (true) {
                    if (eat('%'.code)) {
                        x /= 100.0
                    } else if (eat('!'.code)) {
                        x = factorial(x)
                    } else {
                        break
                    }
                }

                return x
            }

            fun factorial(n: Double): Double {
                if (n < 0.0 || n > 170.0 || n != floor(n)) {
                    throw IllegalArgumentException("Factorial is only defined for non-negative integers up to 170")
                }
                var result = 1.0
                var i = 1.0
                while (i <= n) {
                    result *= i
                    i++
                }
                return result
            }
        }.parse()
    }
}
