
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
    val trackIndex: Int,
    val gapBeforeMs: Long,
    val transitionToNext: TransitionType
)

data class TransitionPlan(
    val clips: List<TransitionClip>,
    val transitionDurationMs: Long,
    val durationMs: Long
) {

    companion object {

        fun create(
            startTimes: List<Long>,
            clipLengthMs: Long,
            detectedBeats: List<Long>
        ): TransitionPlan {

            if (startTimes.isEmpty() || clipLengthMs <= 0L) {
                return TransitionPlan(
                    emptyList(),
                    0L,
                    0L
                )
            }

            val sorted = startTimes
                .filter { it >= 0L }
                .distinct()
                .sorted()

            if (sorted.isEmpty()) {
                return TransitionPlan(
                    emptyList(),
                    0L,
                    0L
                )
            }

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
            }.coerceAtMost(
                (clipLengthMs / 2L).coerceAtLeast(1L)
            )

            val step = clipLengthMs - transitionDuration

            val trackEnds = mutableListOf(0L, 0L)

            val clips = sorted.mapIndexed { index, sourceStart ->

                val trackIndex = index % 2

                val timelineStart = index.toLong() * step
                val timelineEnd = timelineStart + clipLengthMs

                val gapBefore = (
                    timelineStart - trackEnds[trackIndex]
                ).coerceAtLeast(0L)

                trackEnds[trackIndex] = timelineEnd

                TransitionClip(
                    sourceStartMs = sourceStart,
                    sourceEndMs = sourceStart + clipLengthMs,
                    timelineStartMs = timelineStart,
                    timelineEndMs = timelineEnd,
                    trackIndex = trackIndex,
                    gapBeforeMs = gapBefore,
                    transitionToNext = if (
                        index == sorted.lastIndex
                    ) {
                        TransitionType.CUT
                    } else {
                        TransitionType.CROSSFADE
                    }
                )
            }

            return TransitionPlan(
                clips = clips,
                transitionDurationMs = transitionDuration,
                durationMs = clips.last().timelineEndMs
            )
        }
    }
}
