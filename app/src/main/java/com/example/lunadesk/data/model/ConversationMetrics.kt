package com.example.lunadesk.data.model

data class ConversationMetrics(
    val inputTokens: Long?, val inputCount: Int,
    val outputTokens: Long?, val outputCount: Int,
    val speed: Double?, val speedCount: Int,
    val firstTextMs: Double?, val firstTextCount: Int,
    val completedCount: Int, val mixedModels: Boolean
)

fun aggregateMetrics(messages: List<ChatMessageUi>): ConversationMetrics {
    val calls = messages.mapNotNull { it.metrics }.filter { it.completed }
    val inputs = calls.mapNotNull { it.inputTokens?.takeIf { n -> n >= 0 } }
    val outputs = calls.mapNotNull { it.outputTokens?.takeIf { n -> n >= 0 } }
    val speeds = calls.filter { (it.outputTokens ?: 0) > 0 && (it.outputMs ?: 0) >= 100 }
    val firsts = calls.filter { (it.inputTokens ?: 0) > 0 && it.firstTextMs != null }
    val totalDuration = speeds.sumOf { it.outputMs!! }
    val totalWeight = firsts.sumOf { it.inputTokens!!.toLong() }
    return ConversationMetrics(
        inputs.takeIf { it.isNotEmpty() }?.sumOf { it.toLong() }, inputs.size,
        outputs.takeIf { it.isNotEmpty() }?.sumOf { it.toLong() }, outputs.size,
        if (totalDuration > 0) speeds.sumOf { it.outputTokens!!.toLong() } * 1000.0 / totalDuration else null,
        speeds.size,
        if (totalWeight > 0) firsts.sumOf { it.firstTextMs!!.toDouble() * it.inputTokens!! } / totalWeight else null,
        firsts.size, calls.size, calls.mapNotNull { it.model }.distinct().size > 1
    )
}
