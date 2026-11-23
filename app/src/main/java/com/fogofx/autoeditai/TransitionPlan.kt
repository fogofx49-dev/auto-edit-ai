
package com.fogofx.autoeditai

enum class TransitionType {
    CROSSFADE,
    CUT
}

data class TransitionClip(
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val timelineStartMs: Long,
    val timelineEndMs: Long,
    val transitionToNext: TransitionType
)

data class TransitionPlan(
    val clips: List<TransitionClip>,
    val transitionDurationMs: Long
) {

    companion object {

        fun create(
            startTimes: List<Long>,
            clipLengthMs: Long,
            detectedBeats: List<Long>
        ): TransitionPlan {

            if (startTimes.isEmpty()) {
                return TransitionPlan(emptyList(), 0L)
            }

            val sorted = startTimes.distinct().sorted()

            val beatIntervals = detectedBeats
                .sorted()
                .zipWithNext { a, b -> b - a }
                .filter { it in 150L..2000L }

            val averageBeat = if (beatIntervals.isNotEmpty()) {
                beatIntervals.average().toLong()
            } else {
                600L
            }

            val transitionDuration = when {
                averageBeat < 400L -> 250L
                averageBeat < 650L -> 400L
                else -> 600L
            }.coerceAtMost(clipLengthMs / 3)

            var timelinePosition = 0L

            val clips = sorted.mapIndexed { index, start ->

                val isLast = index == sorted.lastIndex

                val sourceEnd = start + clipLengthMs
                val timelineStart = timelinePosition
                val timelineEnd = timelineStart + clipLengthMs

                val clip = TransitionClip(
                    sourceStartMs = start,
                    sourceEndMs = sourceEnd,
                    timelineStartMs = timelineStart,
                    timelineEndMs = timelineEnd,
                    transitionToNext = if (isLast) {
                        TransitionType.CUT
                    } else {
                        TransitionType.CROSSFADE
                    }
                )

                timelinePosition = timelineEnd -
                    if (isLast) 0L else transitionDuration

                clip
            }

            return TransitionPlan(
                clips = clips,
                transitionDurationMs = transitionDuration
            )
        }
    }
}
