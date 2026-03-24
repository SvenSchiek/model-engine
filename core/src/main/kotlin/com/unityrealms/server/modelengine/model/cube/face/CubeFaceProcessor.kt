package com.unityrealms.server.modelengine.model.cube.face

import com.unityrealms.server.modelengine.logger.Logger
import com.unityrealms.server.modelengine.model.ParsedTexture

import java.util.Locale

import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Processes the faces of a cube.
 *
 * @param modelName The name of the model this cube belongs to.
 * @param resolutionHeight The height of the texture of this model.
 * @param resolutionWidth The width of the texture of this model.
 * @param parsedTextureList The list of parsed textures in the model.
 * @param jsonMap The JSON map representing the cube.
 * @param round The function to process the UV coordinates.
 */
class CubeFaceProcessor(
  private val modelName: String?,
  private val resolutionHeight: Double,
  private val resolutionWidth: Double,
  private val parsedTextureList: MutableList<ParsedTexture>,
  private val jsonMap: MutableMap<String?, Any?>,
  private val round: (Float) -> Float
) {

  private var hasFaceTexture: Boolean? = null

  /**
   * Normalizes the faces of the cube by removing any keys that are not strings.
   */
  fun normalizeInPlace() {
    val facesMap: MutableMap<String, Any?>? = (this.jsonMap["faces"] as? Map<*, *>)?.let { map ->
      val normalizedMap = LinkedHashMap<String, Any?>()

      for ((key, value) in map) {
        if (key !is String) continue

        val normalizedValue = when (value) {
          is Map<*, *> -> {
            val innerMap = LinkedHashMap<String, Any?>()

            for ((innerKey, innerValue) in value) {
              if (innerKey is String) {
                innerMap[innerKey] = innerValue
              }
            }

            innerMap
          }
          else -> value
        }

        normalizedMap[key] = normalizedValue
      }

      this.jsonMap["faces"] = normalizedMap

      normalizedMap
    }

    this.processFace("north", facesMap)
    this.processFace("east", facesMap)
    this.processFace("south", facesMap)
    this.processFace("west", facesMap)
    this.processFace("up", facesMap)
    this.processFace("down", facesMap)
  }

  /**
   * Remaps the faces of the cube based on the specified axis and angle.
   *
   * @param axis The axis to remap the faces along.
   * @param angle The angle to rotate the faces by.
   */
  fun remapInPlace(axis: String, angle: Double) {
    val faceTransformationMap: MutableMap<String, Any?> = LinkedHashMap()

    for ((key, value) in (this.jsonMap["faces"] as? Map<*, *> ?: return)) {
      if (key is String) {
        faceTransformationMap[key] = value
      }
    }

    val originalFaceMap: MutableMap<String, Any?> = HashMap(faceTransformationMap)

    when (axis.lowercase(Locale.getDefault())) {
      "x" -> {
        if (abs(angle - 90) < 0.01) {
          faceTransformationMap["north"] = originalFaceMap["down"]
          faceTransformationMap["up"] = originalFaceMap["north"]
          faceTransformationMap["south"] = originalFaceMap["up"]
          faceTransformationMap["down"] = originalFaceMap["south"]

          this.applyRotationToFace("east", 90.0, faceTransformationMap)
          this.applyRotationToFace("west", -90.0, faceTransformationMap)
        } else if (abs(angle + 90) < 0.01) {
          faceTransformationMap["north"] = originalFaceMap["up"]
          faceTransformationMap["down"] = originalFaceMap["north"]
          faceTransformationMap["south"] = originalFaceMap["down"]
          faceTransformationMap["up"] = originalFaceMap["south"]

          this.applyRotationToFace("east", -90.0, faceTransformationMap)
          this.applyRotationToFace("west", 90.0, faceTransformationMap)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          faceTransformationMap["north"] = originalFaceMap["south"]
          faceTransformationMap["south"] = originalFaceMap["north"]
          faceTransformationMap["up"] = originalFaceMap["down"]
          faceTransformationMap["down"] = originalFaceMap["up"]

          this.applyRotationToFace("east", 180.0, faceTransformationMap)
          this.applyRotationToFace("west", 180.0, faceTransformationMap)
        }
      }

      "y" -> {
        if (abs(angle - 90) < 0.01) {
          faceTransformationMap["north"] = originalFaceMap["west"]
          faceTransformationMap["east"] = originalFaceMap["north"]
          faceTransformationMap["south"] = originalFaceMap["east"]
          faceTransformationMap["west"] = originalFaceMap["south"]

          this.applyRotationToFace("up", 90.0, faceTransformationMap)
          this.applyRotationToFace("down", -90.0, faceTransformationMap)
        } else if (abs(angle + 90) < 0.01) {
          faceTransformationMap["north"] = originalFaceMap["east"]
          faceTransformationMap["west"] = originalFaceMap["north"]
          faceTransformationMap["south"] = originalFaceMap["west"]
          faceTransformationMap["east"] = originalFaceMap["south"]

          this.applyRotationToFace("up", -90.0, faceTransformationMap)
          this.applyRotationToFace("down", 90.0, faceTransformationMap)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          faceTransformationMap["north"] = originalFaceMap["south"]
          faceTransformationMap["south"] = originalFaceMap["north"]
          faceTransformationMap["east"] = originalFaceMap["west"]
          faceTransformationMap["west"] = originalFaceMap["east"]

          this.applyRotationToFace("up", 180.0, faceTransformationMap)
          this.applyRotationToFace("down", 180.0, faceTransformationMap)
        }
      }

      "z" -> {
        if (abs(angle - 90) < 0.01) {
          faceTransformationMap["up"] = originalFaceMap["west"]
          faceTransformationMap["east"] = originalFaceMap["up"]
          faceTransformationMap["down"] = originalFaceMap["east"]
          faceTransformationMap["west"] = originalFaceMap["down"]

          this.applyRotationToFace("north", 90.0, faceTransformationMap)
          this.applyRotationToFace("south", -90.0, faceTransformationMap)
        } else if (abs(angle + 90) < 0.01) {
          faceTransformationMap["up"] = originalFaceMap["east"]
          faceTransformationMap["west"] = originalFaceMap["up"]
          faceTransformationMap["down"] = originalFaceMap["west"]
          faceTransformationMap["east"] = originalFaceMap["down"]

          this.applyRotationToFace("north", -90.0, faceTransformationMap)
          this.applyRotationToFace("south", 90.0, faceTransformationMap)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          faceTransformationMap["up"] = originalFaceMap["down"]
          faceTransformationMap["down"] = originalFaceMap["up"]
          faceTransformationMap["east"] = originalFaceMap["west"]
          faceTransformationMap["west"] = originalFaceMap["east"]

          this.applyRotationToFace("north", 180.0, faceTransformationMap)
          this.applyRotationToFace("south", 180.0, faceTransformationMap)
        }
      }
    }

    this.jsonMap["faces"] = faceTransformationMap
  }


  /**
   * Sets the texture data for the specified face.
   *
   * @param facesMap The map containing the faces of the cube.
   */
  private fun processFaceTexture(facesMap: MutableMap<String, Any?>?) {
    if (facesMap == null || facesMap["texture"] == null) {
      if (this.hasFaceTexture == true) {
        Logger.warn("A cube in the model '$modelName' has a face which does not have a texture while the rest of the cube has a texture.")
      }

      this.hasFaceTexture = false

      return
    }

    if (this.hasFaceTexture != null && !(this.hasFaceTexture!!)) {
      Logger.warn("A cube in the model '$modelName' has a face which does not have a texture while the rest of the cube has a texture.")
    }

    this.hasFaceTexture = true

    val faceTextureMap: MutableMap<String, Any?> = LinkedHashMap()

    for ((key, value) in facesMap) {
      faceTextureMap[key] = value
    }

    val textureValue = when (val textureObject = faceTextureMap["texture"]) {
      is Number -> textureObject.toDouble().roundToInt()
      is String -> textureObject.trimStart('#').toIntOrNull() ?: 0
      else -> 0
    }

    faceTextureMap["texture"] = "#$textureValue"
    faceTextureMap["tintindex"] = 0

    faceTextureMap["rotation"] = when (val rotationObject = faceTextureMap["rotation"]) {
      null -> 0
      is Number -> rotationObject.toFloat()
      else -> 0
    }

    val uvList = faceTextureMap["uv"] as? List<*>

    var textureHeight = this.resolutionHeight
    var textureWidth = this.resolutionWidth

    if (textureValue >= 0 && textureValue < this.parsedTextureList.size) {
      val parsedTexture = this.parsedTextureList[textureValue]

      textureHeight = parsedTexture.textureHeight
      textureWidth = parsedTexture.textureWidth
    }

    val uvHeightMultiplier = 16.0 / textureHeight
    val uvWidthMultiplier = 16.0 / textureWidth

    if (uvList != null) {
      val uv0 = (uvList.getOrNull(0) as? Number)?.toDouble()?.toFloat() ?: 0.0F
      val uv1 = (uvList.getOrNull(1) as? Number)?.toDouble()?.toFloat() ?: 0.0F
      val uv2 = (uvList.getOrNull(2) as? Number)?.toDouble()?.toFloat() ?: 0.0F
      val uv3 = (uvList.getOrNull(3) as? Number)?.toDouble()?.toFloat() ?: 0.0F

      faceTextureMap["uv"] = listOf(
        this.round(uv0 * uvWidthMultiplier.toFloat()),
        this.round(uv1 * uvHeightMultiplier.toFloat()),
        this.round(uv2 * uvWidthMultiplier.toFloat()),
        this.round(uv3 * uvHeightMultiplier.toFloat())
      )
    }

    facesMap.clear()

    for ((key, value) in faceTextureMap) {
      facesMap[key] = value
    }
  }

  /**
   * Processes the specified face of the cube.
   *
   * @param faceName The name of the face to process.
   * @param facesMap The map containing the faces of the cube.
   */
  private fun processFace(faceName: String?, facesMap: MutableMap<String, Any?>?) {
    val faceAny = facesMap?.get(faceName)

    this.processFaceTexture(if (faceAny is Map<*, *>) {
      val innerMap = LinkedHashMap<String, Any?>()

      for ((innerKey, innerValue) in faceAny) {
        if (innerKey is String) {
          innerMap[innerKey] = innerValue
        }
      }

      innerMap
    } else {
      null
    })
  }


  /**
   * Applies the specified rotation to the specified face of the cube.
   *
   * @param faceName The name of the face to rotate.
   * @param rotation The rotation angle in degrees.
   * @param facesMap The map containing the faces of the cube.
   */
  private fun applyRotationToFace(faceName: String?, rotation: Double, facesMap: MutableMap<String, Any?>) {
    if (faceName == null) {
      return
    }

    val faceAny = facesMap[faceName] as? Map<*, *> ?: return

    val uvMap: MutableMap<String, Any?> = LinkedHashMap()

    for ((key, value) in faceAny) {
      if (key is String) {
        uvMap[key] = value
      }
    }

    val uvAny = uvMap["uv"]

    val uvList = uvAny as? List<*>

    if (uvList == null || uvList.size != 4) {
      return
    }

    val uv0: Double = (uvList[0] as Number).toDouble()
    val uv1: Double = (uvList[1] as Number).toDouble()
    val uv2: Double = (uvList[2] as Number).toDouble()
    val uv3: Double = (uvList[3] as Number).toDouble()

    uvMap["uv"] = when ((rotation.toInt() % 360 + 360) % 360) {
      90 -> {
        uvMap["rotation"] = 90

        listOf<Double?>(uv0, uv3, uv2, uv1)
      }

      180 -> {
        uvMap["rotation"] = 180

        listOf<Double?>(uv2, uv3, uv0, uv1)
      }

      270 -> {
        uvMap["rotation"] = 270

        listOf<Double?>(uv2, uv1, uv0, uv3)
      }

      else -> uvList
    }

    facesMap[faceName] = uvMap
  }
}
