package com.unityrealms.server.modelengine.model.skeleton.bone.cube

import com.unityrealms.server.modelengine.ModelEngine
import com.unityrealms.server.modelengine.Version
import com.unityrealms.server.modelengine.model.ParsedTexture
import com.unityrealms.server.modelengine.logger.Logger
import com.unityrealms.server.modelengine.model.skeleton.bone.cube.face.CubeFaceProcessor
import com.unityrealms.server.modelengine.model.skeleton.bone.BoneBlueprint

import java.util.Locale

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

import org.joml.Vector3f

/**
 * Represents a single cube in a model.
 *
 * @param parsedTextureList The list of parsed textures in the model.
 * @param modelName The name of the model this cube belongs to.
 * @param resolutionHeight The height of the texture of this model.
 * @param resolutionWidth The width of the texture of this model.
 * @param jsonMap The JSON map representing the cube.
 */
class CubeBlueprint(
  parsedTextureList: MutableList<ParsedTexture>,

  private val modelName: String?,
  private val resolutionHeight: Double,
  private val resolutionWidth: Double,
  private val jsonMap: MutableMap<String?, Any?>
) {

  /**
   * Represents the decomposition of a rotation angle into a base rotation and a remainder.
   */
  private class RotationDecomposition {
    var baseRotation: Double = 0.0
    var remainder: Double = 0.0
  }

  val to: Vector3f

  var from: Vector3f

  var boneOffset: Vector3f = Vector3f()


  private val cubeFaceProcessor: CubeFaceProcessor

  private var validatedData = false

  init {
    this.jsonMap.remove("allow_mirror_modeling")
    this.jsonMap.remove("autouv")
    this.jsonMap.remove("box_uv")
    this.jsonMap.remove("color")
    this.jsonMap.remove("light_emission")
    this.jsonMap.remove("locked")
    this.jsonMap.remove("name")
    this.jsonMap.remove("render_order")
    this.jsonMap.remove("rescale")
    this.jsonMap.remove("type")
    this.jsonMap.remove("uuid")

    this.cubeFaceProcessor = CubeFaceProcessor(
      this.modelName,
      this.resolutionHeight,
      this.resolutionWidth,
      parsedTextureList,
      this.jsonMap,
      this::round
    )

    this.cubeFaceProcessor.normalizeInPlace()

    val rawFromList = this.jsonMap["from"] as? List<*>

    if (rawFromList == null) {
      Logger.warn("Model '${this.modelName}' has a cube with no 'from' position.")
    }

    val from0 = (rawFromList?.getOrNull(0) as? Number)?.toDouble()?.toFloat() ?: 0.0F
    val from1 = (rawFromList?.getOrNull(1) as? Number)?.toDouble()?.toFloat() ?: 0.0F
    val from2 = (rawFromList?.getOrNull(2) as? Number)?.toDouble()?.toFloat() ?: 0.0F

    this.from = Vector3f(
      this.round(from0 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER),
      this.round(from1 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER),
      this.round(from2 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER)
    )

    val rawToList = this.jsonMap["to"] as? List<*>

    val to0 = (rawToList?.getOrNull(0) as? Number)?.toDouble()?.toFloat() ?: from0
    val to1 = (rawToList?.getOrNull(1) as? Number)?.toDouble()?.toFloat() ?: from1
    val to2 = (rawToList?.getOrNull(2) as? Number)?.toDouble()?.toFloat() ?: from2

    this.from = Vector3f(
      from0 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER,
      from1 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER,
      from2 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER
    )

    this.to = Vector3f(
      to0 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER,
      to1 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER,
      to2 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER
    )

    this.applyInflate()

    this.validatedData = true
  }


  /**
   * Shifts the position of the cube by the bone offset.
   */
  fun shiftPosition() {
    this.from.sub(this.boneOffset)
    this.to.sub(this.boneOffset)

    this.updateFromAndToInJsonMap()
  }

  /**
   * Shifts the rotation of the cube by the bone offset.
   */
  fun shiftRotation() {
    if (this.jsonMap["origin"] == null) {
      return
    }

    val newRotationMap: MutableMap<String?, Any?> = HashMap()

    val scaleFactor = BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER.toDouble()

    val originX: Double
    val originY: Double
    val originZ: Double

    val originRawList = this.jsonMap["origin"] as? List<*> ?: return

    originX = (originRawList.getOrNull(0) as? Number)?.toDouble()?.times(scaleFactor)?.minus(this.boneOffset.get(0)) ?: return
    originY = (originRawList.getOrNull(1) as? Number)?.toDouble()?.times(scaleFactor)?.minus(this.boneOffset.get(1)) ?: return
    originZ = (originRawList.getOrNull(2) as? Number)?.toDouble()?.times(scaleFactor)?.minus(this.boneOffset.get(2)) ?: return

    var axis = "x"

    var angle = 0.0

    if (this.jsonMap["rotation"] != null) {
      val rotationRawList = this.jsonMap["rotation"] as? List<*>

      if (rotationRawList != null) {
        for (index in rotationRawList.indices.reversed()) {
          val rotationObject = (rotationRawList.getOrNull(index) as? Number)?.toDouble() ?: 0.0

          if (rotationObject != 0.0) {
            when (index) {
              0 -> axis = "x"
              1 -> axis = "y"
              2 -> axis = "z"
              else -> Logger.warn("An unexpected number of rotation axes has been found.")
            }

            angle = this.round(rotationObject.toFloat()).toDouble()
          }
        }
      }
    }

    val rotationDecomposition = this.decomposeRotation(angle)

    if (rotationDecomposition.baseRotation != 0.0) {
      this.transformCubeGeometry(axis, rotationDecomposition.baseRotation, originX, originY, originZ)

      newRotationMap["axis"] = axis
      newRotationMap["angle"] = rotationDecomposition.remainder
    } else {
      newRotationMap["axis"] = axis
      newRotationMap["angle"] = angle
    }

    newRotationMap["origin"] = listOf<Double?>(originX, originY, originZ)

    this.jsonMap["rotation"] = newRotationMap
    this.jsonMap.remove("origin")
  }


  /**
   * Rounds a value to 4 decimal places.
   *
   * @param value The value to round.
   *
   * @return The rounded value.
   */
  private fun round(value: Float): Float {
    return (kotlin.math.round(value * 10000.0F) / 10000.0F)
  }

  /**
   * Updates the 'from' and 'to' positions in the JSON map.
   */
  private fun updateFromAndToInJsonMap() {
    this.jsonMap["from"] = listOf<Float?>(this.from.get(0), this.from.get(1), this.from.get(2))
    this.jsonMap["to"] = listOf<Float?>(this.to.get(0), this.to.get(1), this.to.get(2))
  }


  /**
   * Applies the "inflate" property to the geometry of the cube if it exists.
   */
  private fun applyInflate() {
    val inflateObject = this.jsonMap["inflate"] ?: return

    val inflateRawValue = (inflateObject as Number).toDouble()

    if (abs(inflateRawValue) < 1e-9) {
      this.jsonMap.remove("inflate")

      return
    }

    val inflate: Float = this.round((inflateRawValue * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER).toFloat())

    val newFrom: Vector3f = Vector3f(this.from).sub(inflate, inflate, inflate)
    val newTo: Vector3f = Vector3f(this.to).add(inflate, inflate, inflate)

    if (newFrom.x > newTo.x || newFrom.y > newTo.y || newFrom.z > newTo.z) {
      Logger.warn("Inflate on model '${this.modelName}' is too negative and would invert the cube.")

      val inflationAmount = 0.0001F

      if (newFrom.x > newTo.x) {
        val center = (this.from.x + this.to.x) * 0.5F

        newFrom.x = center - inflationAmount
        newTo.x = center + inflationAmount
      }

      if (newFrom.y > newTo.y) {
        val center = (this.from.y + this.to.y) * 0.5F

        newFrom.y = center - inflationAmount
        newTo.y = center + inflationAmount
      }

      if (newFrom.z > newTo.z) {
        val center = (this.from.z + this.to.z) * 0.5F

        newFrom.z = center - inflationAmount
        newTo.z = center + inflationAmount
      }
    }

    this.from.set(newFrom)
    this.to.set(newTo)

    this.jsonMap["from"] = listOf<Float?>(
      this.round(this.from.get(0)),
      this.round(this.from.get(1)),
      this.round(this.from.get(2))
    )

    this.jsonMap["to"] = listOf<Float?>(
      this.round(this.to.get(0)),
      this.round(this.to.get(1)),
      this.round(this.to.get(2))
    )

    this.jsonMap.remove("inflate")
  }


  /**
   * Decomposes a rotation angle into its base rotation and remainder.
   *
   * @param angle The angle to decompose.
   *
   * @return A [RotationDecomposition] object containing the base rotation and remainder.
   */
  private fun decomposeRotation(angle: Double): RotationDecomposition {
    var angle = angle

    val rotationDecomposition = RotationDecomposition()

    while (angle > 180) angle -= 360.0
    while (angle < -180) angle += 360.0

    val allowedRotationArray = doubleArrayOf(-45.0, -22.5, 0.0, 22.5, 45.0)

    val baseRotationArray = doubleArrayOf(0.0, 90.0, -90.0, 180.0, -180.0)

    for (baseRotation in baseRotationArray) {
      var remainder = angle - baseRotation

      while (remainder > 180) remainder -= 360.0
      while (remainder < -180) remainder += 360.0

      if (ModelEngine.version >= Version.fromString("1.21.6")) {
        if (remainder >= -45 && remainder <= 45) {
          rotationDecomposition.baseRotation = baseRotation
          rotationDecomposition.remainder = remainder

          return rotationDecomposition
        }
      } else {
        for (allowedRotation in allowedRotationArray) {
          if (abs(remainder - allowedRotation) < 0.01) {
            rotationDecomposition.baseRotation = baseRotation
            rotationDecomposition.remainder = allowedRotation

            return rotationDecomposition
          }
        }
      }
    }

    Logger.warn("Could not decompose rotation angle $angle into base and allowed a remainder for model '${this.modelName}'.")

    rotationDecomposition.baseRotation = 0.0
    rotationDecomposition.remainder = angle

    return rotationDecomposition
  }


  /**
   * Transforms the geometry of a cube based on the given rotation angle and axis.
   *
   * @param axis The axis of rotation (e.g., "x", "y", "z").
   * @param angle The rotation angle in degrees.
   * @param originX The X coordinate of the origin.
   * @param originY The Y coordinate of the origin.
   * @param originZ The Z coordinate of the origin.
   */
  private fun transformCubeGeometry(axis: String, angle: Double, originX: Double, originY: Double, originZ: Double) {
    var fromX = this.from.x
    var fromY = this.from.y
    var fromZ = this.from.z

    var toX = this.to.x
    var toY = this.to.y
    var toZ = this.to.z

    fromX -= originX.toFloat()
    fromY -= originY.toFloat()
    fromZ -= originZ.toFloat()

    toX -= originX.toFloat()
    toY -= originY.toFloat()
    toZ -= originZ.toFloat()

    val newFrom = Vector3f()
    val newTo = Vector3f()

    when (axis.lowercase(Locale.getDefault())) {
      "x" -> {
        if (abs(angle - 90) < 0.01) {
          newFrom.set(fromX, -fromZ, fromY)
          newTo.set(toX, -toZ, toY)
        } else if (abs(angle + 90) < 0.01) {
          newFrom.set(fromX, fromZ, -fromY)
          newTo.set(toX, toZ, -toY)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          newFrom.set(fromX, -fromY, -fromZ)
          newTo.set(toX, -toY, -toZ)
        }
      }

      "y" -> {
        if (abs(angle - 90) < 0.01) {
          newFrom.set(fromZ, fromY, -fromX)
          newTo.set(toZ, toY, -toX)
        } else if (abs(angle + 90) < 0.01) {
          newFrom.set(-fromZ, fromY, fromX)
          newTo.set(-toZ, toY, toX)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          newFrom.set(-fromX, fromY, -fromZ)
          newTo.set(-toX, toY, -toZ)
        }
      }

      "z" -> {
        if (abs(angle - 90) < 0.01) {
          newFrom.set(-fromY, fromX, fromZ)
          newTo.set(-toY, toX, toZ)
        } else if (abs(angle + 90) < 0.01) {
          newFrom.set(fromY, -fromX, fromZ)
          newTo.set(toY, -toX, toZ)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          newFrom.set(-fromX, -fromY, fromZ)
          newTo.set(-toX, -toY, toZ)
        }
      }
    }

    newFrom.add(originX.toFloat(), originY.toFloat(), originZ.toFloat())
    newTo.add(originX.toFloat(), originY.toFloat(), originZ.toFloat())

    this.from.set(
      min(newFrom.x, newTo.x),
      min(newFrom.y, newTo.y),
      min(newFrom.z, newTo.z)
    )

    this.to.set(
      max(newFrom.x, newTo.x),
      max(newFrom.y, newTo.y),
      max(newFrom.z, newTo.z)
    )

    this.updateFromAndToInJsonMap()

    this.cubeFaceProcessor.remapInPlace(axis, angle)
  }
}
