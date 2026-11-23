
package com.fogofx.autoeditai

import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.StaticOverlaySettings

@UnstableApi
class CrossfadeVideoCompositorSettings(
    private val plan: TransitionPlan
) : VideoCompositorSettings {

    override fun getOutputSize(inputSizes: List<Size>): Size {
        return inputSizes.first()
    }

    override fun getOverlaySettings(
        inputId: Int,
        presentationTimeUs: Long
    ): OverlaySettings {

        val timeMs = presentationTimeUs / 1000L

        val currentClip = plan.clips.firstOrNull {
            it.trackIndex == inputId &&
            timeMs >= it.timelineStartMs &&
            timeMs < it.timelineEndMs
        }

        if (currentClip == null) {
            return StaticOverlaySettings.Builder()
                .setAlphaScale(0f)
                .build()
        }

        val otherClip = plan.clips.firstOrNull {
            it.trackIndex != inputId &&
            timeMs >= it.timelineStartMs &&
            timeMs < it.timelineEndMs
        }

        val alpha = if (otherClip == null) {
            1f
        } else {
            val overlapStart = maxOf(
                currentClip.timelineStartMs,
                otherClip.timelineStartMs
            )

            val overlapEnd = minOf(
                currentClip.timelineEndMs,
                otherClip.timelineEndMs
            )

            if (
                currentClip.timelineStartMs >
                otherClip.timelineStartMs &&
                overlapEnd > overlapStart
            ) {
                (
                    (timeMs - overlapStart).toFloat() /
                    (overlapEnd - overlapStart).toFloat()
                ).coerceIn(0f, 1f)
            } else {
                1f
            }
        }

        return StaticOverlaySettings.Builder()
            .setAlphaScale(alpha)
            .build()
    }
}
