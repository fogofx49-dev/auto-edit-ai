
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

        val track0Clip = plan.clips.firstOrNull {
            it.trackIndex == 0 &&
            timeMs >= it.timelineStartMs &&
            timeMs < it.timelineEndMs
        }

        val track1Clip = plan.clips.firstOrNull {
            it.trackIndex == 1 &&
            timeMs >= it.timelineStartMs &&
            timeMs < it.timelineEndMs
        }

        val alpha = when (inputId) {

            // FAIXA INFERIOR
            0 -> {
                if (track0Clip == null) {
                    0f
                } else {
                    1f
                }
            }

            // FAIXA SUPERIOR
            1 -> {

                if (track1Clip == null) {
                    0f

                } else if (track0Clip == null) {
                    1f

                } else {

                    val overlapStart = maxOf(
                        track0Clip.timelineStartMs,
                        track1Clip.timelineStartMs
                    )

                    val overlapEnd = minOf(
                        track0Clip.timelineEndMs,
                        track1Clip.timelineEndMs
                    )

                    if (overlapEnd <= overlapStart) {
                        1f
                    } else {

                        val progress = (
                            (timeMs - overlapStart).toFloat() /
                            (overlapEnd - overlapStart).toFloat()
                        ).coerceIn(0f, 1f)

                        if (
                            track1Clip.timelineStartMs >
                            track0Clip.timelineStartMs
                        ) {
                            // A faixa superior é o novo clipe.
                            progress
                        } else {
                            // A faixa superior é o clipe anterior.
                            1f - progress
                        }
                    }
                }
            }

            else -> 0f
        }

        return StaticOverlaySettings.Builder()
            .setAlphaScale(alpha)
            .build()
    }
}
