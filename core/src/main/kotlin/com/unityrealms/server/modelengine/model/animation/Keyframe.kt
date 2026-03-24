package com.unityrealms.server.modelengine.model.animation

import com.magmaguy.magmacore.util.Logger

class Keyframe {
  @Getter
  private val transformationType: TransformationType?

  @Getter
  private val timeInTicks: kotlin.Int

  @Getter
  private val interpolationType: InterpolationType?

  @Getter
  private val dataX: kotlin.Float

  @Getter
  private val dataY: kotlin.Float

  @Getter
  private val dataZ: kotlin.Float

  constructor(`object`: Any?, modelName: kotlin.String?, animationName: kotlin.String?) {
    val data = `object` as kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?>
    transformationType =
      TransformationType.valueOf((data.get("channel") as kotlin.String).uppercase(Locale.getDefault()))
    interpolationType =
      InterpolationType.valueOf((data.get("interpolation") as kotlin.String).uppercase(Locale.getDefault()))
    timeInTicks = (20 * data.get("time") as kotlin.Double).toInt()
    val dataPoints =
      (data.get("data_points") as kotlin.collections.MutableList<kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?>>).get(
        0
      )

    dataX = tryParseFloat(dataPoints.get("x")!!, modelName, animationName)
    dataY = tryParseFloat(dataPoints.get("y")!!, modelName, animationName)
    dataZ = tryParseFloat(dataPoints.get("z")!!, modelName, animationName)
  }

  constructor(
    transformationType: TransformationType?,
    timeInTicks: kotlin.Int,
    interpolationType: InterpolationType?,
    dataX: kotlin.Float,
    dataY: kotlin.Float,
    dataZ: kotlin.Float
  ) {
    this.transformationType = transformationType
    this.timeInTicks = timeInTicks
    this.interpolationType = interpolationType
    this.dataX = dataX
    this.dataY = dataY
    this.dataZ = dataZ
  }

  private fun tryParseFloat(
    rawObject: kotlin.Any,
    modelName: kotlin.String?,
    animationName: kotlin.String?
  ): kotlin.Float {
    if (rawObject !is kotlin.String) return (rawObject as kotlin.Double).toFloat()
    rawObject = rawObject.replace("\\n".toRegex(), "")
    if (rawObject.isEmpty()) return if (transformationType == TransformationType.SCALE) 1f else 0f
    try {
      return rawObject.toDouble().toFloat()
    } catch (e: java.lang.Exception) {
      Logger.warn("Failed to parse supposed number value " + rawObject + " in animation " + animationName + " for model " + modelName + "!")
      return 0f
    }
  }
}
