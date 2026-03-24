package com.unityrealms.server.modelengine.model.cube

import com.unityrealms.server.modelengine.ModelEngine
import com.unityrealms.server.modelengine.Version
import com.unityrealms.server.modelengine.model.ParsedTexture
import com.unityrealms.server.modelengine.logger.Logger
import com.unityrealms.server.modelengine.model.skeleton.bone.BoneBlueprint

import java.util.Locale

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

import org.joml.Vector3f

/**
 * Represents a single cube in a model.
 *
 * @param parsedTextureList The list of parsed textures in the model.
 * @param modelName The name of the model this cube belongs to.
 * @param resolutionWidth The width of the texture of this model.
 * @param resolutionHeight The height of the texture of this model.
 * @param cubeJsonMap The JSON map representing the cube.
 */
class CubeBlueprint(
  parsedTextureList: MutableList<ParsedTexture>,

  private val modelName: String?,
  private val resolutionWidth: Double,
  private val resolutionHeight: Double,
  private val cubeJsonMap: MutableMap<String?, Any?>
) {

  /**
   * Represents the decomposition of a rotation angle into a base rotation and a remainder.
   */
  private class RotationDecomposition {
    var baseRotation: Double = 0.0
    var remainder: Double = 0.0
  }


  private val boneOffset: Vector3f = Vector3f()


  private val to: Vector3f

  private var from: Vector3f


  private var hasFaceTexture: Boolean? = null


  private var validatedData = false

  init {
    this.cubeJsonMap.remove("allow_mirror_modeling")
    this.cubeJsonMap.remove("autouv")
    this.cubeJsonMap.remove("box_uv")
    this.cubeJsonMap.remove("color")
    this.cubeJsonMap.remove("light_emission")
    this.cubeJsonMap.remove("locked")
    this.cubeJsonMap.remove("name")
    this.cubeJsonMap.remove("render_order")
    this.cubeJsonMap.remove("rescale")
    this.cubeJsonMap.remove("type")
    this.cubeJsonMap.remove("uuid")

    val facesMap: MutableMap<String, Any?>? = (this.cubeJsonMap["faces"] as? Map<*, *>)?.let { map ->
      val linkedMap = LinkedHashMap<String, Any?>()

      for ((key, value) in map) {
        if (key !is String) continue
      
        val value = when (value) {
          is Map<*, *> -> {
            val innerLinkedMap = LinkedHashMap<String, Any?>()

            for ((innerKey, innerValue) in value) {
              if (innerKey is String) {
                innerLinkedMap[innerKey] = innerValue
              }
            }

            innerLinkedMap
          }
          else -> value
        }
      
        linkedMap[key] = value
      }
      
      this.cubeJsonMap["faces"] = linkedMap

      linkedMap
    }

    this.processFace(this.modelName, "north", parsedTextureList, facesMap)
    this.processFace(this.modelName, "east", parsedTextureList, facesMap)
    this.processFace(this.modelName, "south", parsedTextureList, facesMap)
    this.processFace(this.modelName, "west", parsedTextureList, facesMap)
    this.processFace(this.modelName, "up", parsedTextureList, facesMap)
    this.processFace(this.modelName, "down", parsedTextureList, facesMap)

    val fromRawList = this.cubeJsonMap["from"] as? List<*>

    if (fromRawList == null) {
      Logger.warn("Model '${this.modelName}' has a cube with no 'from' position.")
    }

    val from0 = (fromRawList?.getOrNull(0) as? Number)?.toDouble()?.toFloat() ?: 0.0F
    val from1 = (fromRawList?.getOrNull(1) as? Number)?.toDouble()?.toFloat() ?: 0.0F
    val from2 = (fromRawList?.getOrNull(2) as? Number)?.toDouble()?.toFloat() ?: 0.0F

    this.from = Vector3f(
      this.roundToFourDecimalPlaces(from0 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER),
      this.roundToFourDecimalPlaces(from1 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER),
      this.roundToFourDecimalPlaces(from2 * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER)
    )

    val toRawList = this.cubeJsonMap["to"] as? List<*>

    val to0 = (toRawList?.getOrNull(0) as? Number)?.toDouble()?.toFloat() ?: from0
    val to1 = (toRawList?.getOrNull(1) as? Number)?.toDouble()?.toFloat() ?: from1
    val to2 = (toRawList?.getOrNull(2) as? Number)?.toDouble()?.toFloat() ?: from2

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
   * Sets the texture data for a face of a cube.
   *
   * @param modelName The name of the model the cube belongs to.
   * @param parsedTextureList The list of parsed textures.
   * @param facesMap The map containing the face data.
   */
  private fun setTextureData(
    modelName: String?,
    parsedTextureList: MutableList<ParsedTexture>,
    facesMap: MutableMap<String, Any?>?
  ) {
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

    val map: MutableMap<String, Any?> = LinkedHashMap()

    for ((key, value) in facesMap) {
      map[key] = value
    }

    val textureValue = when (val textureObject = map["texture"]) {
      is Number -> textureObject.toDouble().roundToInt()
      is String -> textureObject.trimStart('#').toIntOrNull() ?: 0
      else -> 0
    }

    map["texture"] = "#$textureValue"
    map["tintindex"] = 0

    map["rotation"] = when (val rotationObject = map["rotation"]) {
      null -> 0
      is Number -> rotationObject.toFloat()
      else -> 0
    }

    val uvList = map["uv"] as? List<*>

    var textureWidth = this.resolutionWidth
    var textureHeight = this.resolutionHeight

    if (textureValue >= 0 && textureValue < parsedTextureList.size) {
      val parsedTexture: ParsedTexture = parsedTextureList[textureValue]

      textureWidth = parsedTexture.textureWidth
      textureHeight = parsedTexture.textureHeight
    }
    
    val uvWidthMultiplier = 16.0 / textureWidth
    val uvHeightMultiplier = 16.0 / textureHeight
    
    if (uvList != null) {
      val uv0 = (uvList.getOrNull(0) as? Number)?.toDouble()?.toFloat() ?: 0.0F
      val uv1 = (uvList.getOrNull(1) as? Number)?.toDouble()?.toFloat() ?: 0.0F
      val uv2 = (uvList.getOrNull(2) as? Number)?.toDouble()?.toFloat() ?: 0.0F
      val uv3 = (uvList.getOrNull(3) as? Number)?.toDouble()?.toFloat() ?: 0.0F

      map["uv"] = listOf(
        this.roundToFourDecimalPlaces(uv0 * uvWidthMultiplier.toFloat()),
        this.roundToFourDecimalPlaces(uv1 * uvHeightMultiplier.toFloat()),
        this.roundToFourDecimalPlaces(uv2 * uvWidthMultiplier.toFloat()),
        this.roundToFourDecimalPlaces(uv3 * uvHeightMultiplier.toFloat())
      )
    }

    facesMap.clear()

    for ((key, value) in map) {
      facesMap[key] = value
    }
  }

  /**
   * Processes a face of a cube.
   *
   * @param modelName The name of the model the cube belongs to.
   * @param faceName The name of the face to process.
   * @param parsedTextureList The list of parsed textures.
   * @param facesMap The map containing the faces.
   */
  private fun processFace(
    modelName: String?,
    faceName: String?,
    parsedTextureList: MutableList<ParsedTexture>,
    facesMap: MutableMap<String, Any?>?
  ) {
    val faceAny = facesMap?.get(faceName)

    val typedFacesMap: MutableMap<String, Any?>? = if (faceAny is Map<*, *>) {
      val innerMap = LinkedHashMap<String, Any?>()

      for ((innerKey, innerValue) in faceAny) {
        if (innerKey is String) {
          innerMap[innerKey] = innerValue
        }
      }

      innerMap
    } else {
      null
    }

    this.setTextureData(modelName, parsedTextureList, typedFacesMap)
  }


  /**
   * Shifts the position of the cube by the bone offset.
   */
  fun shiftPosition() {
    this.to.sub(this.boneOffset)
    this.from.sub(this.boneOffset)

    this.cubeJsonMap["to"] = listOf<Float?>(this.to.get(0), this.to.get(1), this.to.get(2))
    this.cubeJsonMap["from"] = listOf<Float?>(this.from.get(0), this.from.get(1), this.from.get(2))
  }

  /**
   * Shifts the rotation of the cube by the bone offset.
   */
  fun shiftRotation() {
    if (this.cubeJsonMap["origin"] == null) return

    val newRotationMap: MutableMap<String?, Any?> = HashMap()

    val scaleFactor = BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER.toDouble()

    val originX: Double
    val originY: Double
    val originZ: Double

    val originRawList = this.cubeJsonMap["origin"] as? List<*> ?: return

    originX = (originRawList.getOrNull(0) as? Number)?.toDouble()?.times(scaleFactor)?.minus(this.boneOffset.get(0)) ?: return
    originY = (originRawList.getOrNull(1) as? Number)?.toDouble()?.times(scaleFactor)?.minus(this.boneOffset.get(1)) ?: return
    originZ = (originRawList.getOrNull(2) as? Number)?.toDouble()?.times(scaleFactor)?.minus(this.boneOffset.get(2)) ?: return

    var angle = 0.0

    var axis = "x"

    if (this.cubeJsonMap["rotation"] != null) {
      val rotationRawList = this.cubeJsonMap["rotation"] as? List<*>

      if (rotationRawList != null) {
        for (index in rotationRawList.indices.reversed()) {
          val rotationObject = (rotationRawList.getOrNull(index) as? Number)?.toDouble() ?: 0.0

          if (rotationObject != 0.0) {
            angle = this.roundToFourDecimalPlaces(rotationObject.toFloat()).toDouble()

            when (index) {
              0 -> axis = "x"
              1 -> axis = "y"
              2 -> axis = "z"
              else -> Logger.warn("An unexpected number of rotation axes has been found.")
            }
          }
        }
      }
    }

    val rotationDecomposition = this.decomposeRotation(angle)

    if (rotationDecomposition.baseRotation != 0.0) {
      this.transformCubeGeometry(rotationDecomposition.baseRotation, axis, originX, originY, originZ)

      newRotationMap["angle"] = rotationDecomposition.remainder
      newRotationMap["axis"] = axis
    } else {
      newRotationMap["angle"] = angle
      newRotationMap["axis"] = axis
    }

    newRotationMap["origin"] = listOf<Double?>(originX, originY, originZ)

    this.cubeJsonMap["rotation"] = newRotationMap
    this.cubeJsonMap.remove("origin")
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

    Logger.warn("Could not decompose rotation angle $angle into base and allowed a remainder for model '$modelName'.")

    rotationDecomposition.baseRotation = 0.0
    rotationDecomposition.remainder = angle

    return rotationDecomposition
  }

  /**
   * Transforms the geometry of a cube based on the given rotation angle and axis.
   *
   * @param angle The rotation angle in degrees.
   * @param axis The axis of rotation (e.g., "x", "y", "z").
   * @param originX The X coordinate of the origin.
   * @param originY The Y coordinate of the origin.
   * @param originZ The Z coordinate of the origin.
   */
  private fun transformCubeGeometry(angle: Double, axis: String, originX: Double, originY: Double, originZ: Double) {
    var toX = this.to.x
    var toY = this.to.y
    var toZ = this.to.z

    var fromX = this.from.x
    var fromY = this.from.y
    var fromZ = this.from.z

    toX -= originX.toFloat()
    toY -= originY.toFloat()
    toZ -= originZ.toFloat()

    fromX -= originX.toFloat()
    fromY -= originY.toFloat()
    fromZ -= originZ.toFloat()

    val newTo = Vector3f()
    val newFrom = Vector3f()

    when (axis.lowercase(Locale.getDefault())) {
      "x" -> {
        if (abs(angle - 90) < 0.01) {
          newTo.set(toX, -toZ, toY)
          newFrom.set(fromX, -fromZ, fromY)
        } else if (abs(angle + 90) < 0.01) {
          newTo.set(toX, toZ, -toY)
          newFrom.set(fromX, fromZ, -fromY)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          newTo.set(toX, -toY, -toZ)
          newFrom.set(fromX, -fromY, -fromZ)
        }
      }

      "y" -> {
        if (abs(angle - 90) < 0.01) {
          // 90 degrees around Y: x' = z, z' = -x
          newFrom.set(fromZ, fromY, -fromX)
          newTo.set(toZ, toY, -toX)
        } else if (abs(angle + 90) < 0.01) {
          // -90 degrees around Y: x' = -z, z' = x
          newFrom.set(-fromZ, fromY, fromX)
          newTo.set(-toZ, toY, toX)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          // 180 degrees around Y: x' = -x, z' = -z
          newFrom.set(-fromX, fromY, -fromZ)
          newTo.set(-toX, toY, -toZ)
        }
      }

      "z" -> {
        if (abs(angle - 90) < 0.01) {
          // 90 degrees around Z: x' = -y, y' = x
          newFrom.set(-fromY, fromX, fromZ)
          newTo.set(-toY, toX, toZ)
        } else if (abs(angle + 90) < 0.01) {
          // -90 degrees around Z: x' = y, y' = -x
          newFrom.set(fromY, -fromX, fromZ)
          newTo.set(toY, -toX, toZ)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          // 180 degrees around Z: x' = -x, y' = -y
          newFrom.set(-fromX, -fromY, fromZ)
          newTo.set(-toX, -toY, toZ)
        }
      }
    }

    // Translate back from origin
    newFrom.add(originX.toFloat(), originY.toFloat(), originZ.toFloat())
    newTo.add(originX.toFloat(), originY.toFloat(), originZ.toFloat())

    // Ensure from is always the minimum corner and to is the maximum
    from.set(
      min(newFrom.x, newTo.x),
      min(newFrom.y, newTo.y),
      min(newFrom.z, newTo.z)
    )
    to.set(
      max(newFrom.x, newTo.x),
      max(newFrom.y, newTo.y),
      max(newFrom.z, newTo.z)
    )

    // Update the JSON data
    cubeJsonMap.put("from", listOf<Float?>(from.get(0), from.get(1), from.get(2)))
    cubeJsonMap.put("to", listOf<Float?>(to.get(0), to.get(1), to.get(2)))

    // Remap faces according to the rotation
    remapFaces(angle, axis)
  }

  private fun remapFaces(angle: Double, axis: String) {
    val facesAny = cubeJsonMap["faces"] as? Map<*, *> ?: return

    // Work on typed mutable copies
    val facesMutable: MutableMap<String, Any?> = LinkedHashMap()
    for ((k, v) in facesAny) if (k is String) facesMutable[k] = v
    val originalFaces: MutableMap<String, Any?> = HashMap(facesMutable)

    when (axis.lowercase(Locale.getDefault())) {
      "x" -> {
        if (abs(angle - 90) < 0.01) {
          facesMutable["north"] = originalFaces["down"]
          facesMutable["up"] = originalFaces["north"]
          facesMutable["south"] = originalFaces["up"]
          facesMutable["down"] = originalFaces["south"]
          rotateFaceUV(facesMutable, "east", 90.0)
          rotateFaceUV(facesMutable, "west", -90.0)
        } else if (abs(angle + 90) < 0.01) {
          facesMutable["north"] = originalFaces["up"]
          facesMutable["down"] = originalFaces["north"]
          facesMutable["south"] = originalFaces["down"]
          facesMutable["up"] = originalFaces["south"]
          rotateFaceUV(facesMutable, "east", -90.0)
          rotateFaceUV(facesMutable, "west", 90.0)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          facesMutable["north"] = originalFaces["south"]
          facesMutable["south"] = originalFaces["north"]
          facesMutable["up"] = originalFaces["down"]
          facesMutable["down"] = originalFaces["up"]
          rotateFaceUV(facesMutable, "east", 180.0)
          rotateFaceUV(facesMutable, "west", 180.0)
        }
      }

      "y" -> {
        if (abs(angle - 90) < 0.01) {
          facesMutable["north"] = originalFaces["west"]
          facesMutable["east"] = originalFaces["north"]
          facesMutable["south"] = originalFaces["east"]
          facesMutable["west"] = originalFaces["south"]
          rotateFaceUV(facesMutable, "up", 90.0)
          rotateFaceUV(facesMutable, "down", -90.0)
        } else if (abs(angle + 90) < 0.01) {
          facesMutable["north"] = originalFaces["east"]
          facesMutable["west"] = originalFaces["north"]
          facesMutable["south"] = originalFaces["west"]
          facesMutable["east"] = originalFaces["south"]
          rotateFaceUV(facesMutable, "up", -90.0)
          rotateFaceUV(facesMutable, "down", 90.0)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          facesMutable["north"] = originalFaces["south"]
          facesMutable["south"] = originalFaces["north"]
          facesMutable["east"] = originalFaces["west"]
          facesMutable["west"] = originalFaces["east"]
          rotateFaceUV(facesMutable, "up", 180.0)
          rotateFaceUV(facesMutable, "down", 180.0)
        }
      }

      "z" -> {
        if (abs(angle - 90) < 0.01) {
          facesMutable["up"] = originalFaces["west"]
          facesMutable["east"] = originalFaces["up"]
          facesMutable["down"] = originalFaces["east"]
          facesMutable["west"] = originalFaces["down"]
          rotateFaceUV(facesMutable, "north", 90.0)
          rotateFaceUV(facesMutable, "south", -90.0)
        } else if (abs(angle + 90) < 0.01) {
          facesMutable["up"] = originalFaces["east"]
          facesMutable["west"] = originalFaces["up"]
          facesMutable["down"] = originalFaces["west"]
          facesMutable["east"] = originalFaces["down"]
          rotateFaceUV(facesMutable, "north", -90.0)
          rotateFaceUV(facesMutable, "south", 90.0)
        } else if (abs(angle - 180) < 0.01 || abs(angle + 180) < 0.01) {
          facesMutable["up"] = originalFaces["down"]
          facesMutable["down"] = originalFaces["up"]
          facesMutable["east"] = originalFaces["west"]
          facesMutable["west"] = originalFaces["east"]
          rotateFaceUV(facesMutable, "north", 180.0)
          rotateFaceUV(facesMutable, "south", 180.0)
        }
      }
    }

    // write back into cubeJsonMap
    cubeJsonMap["faces"] = facesMutable
  }

  //todo: this doesn't actually work correctly for +90 -90 rotations, maybe 180 as well
  private fun rotateFaceUV(faces: MutableMap<String, Any?>, faceName: String?, rotation: Double) {
    if (faceName == null) return
    val faceAny = faces[faceName] as? Map<*, *> ?: return

    val faceMut: MutableMap<String, Any?> = LinkedHashMap()
    for ((k, v) in faceAny) if (k is String) faceMut[k] = v

    val uvAny = faceMut["uv"]
    val uv = uvAny as? List<*>
    if (uv == null || uv.size != 4) return

    val u1: Double = (uv[0] as Number).toDouble()
    val v1: Double = (uv[1] as Number).toDouble()
    val u2: Double = (uv[2] as Number).toDouble()
    val v2: Double = (uv[3] as Number).toDouble()

    val normalizedRotation = (rotation.toInt() % 360 + 360) % 360

    val newUV: Any = when (normalizedRotation) {
      90 -> {
        faceMut["rotation"] = 90
        listOf<Double?>(u1, v2, u2, v1)
      }
      180 -> {
        faceMut["rotation"] = 180
        listOf<Double?>(u2, v2, u1, v1)
      }
      270 -> {
        faceMut["rotation"] = 270
        listOf<Double?>(u2, v1, u1, v2)
      }
      else -> uv
    }

    faceMut["uv"] = newUV

    // write back to faces map
    faces[faceName] = faceMut
  }


  /**
   * Rounds a value to 4 decimal places.
   *
   * @param value The value to round.
   *
   * @return The rounded value.
   */
  private fun roundToFourDecimalPlaces(value: Float): Float {
    return (kotlin.math.round(value * 10000.0F) / 10000.0F)
  }


  /**
   * Applies the "inflate" property to the geometry of the cube if it exists.
   */
  private fun applyInflate() {
    val inflateObject = this.cubeJsonMap["inflate"] ?: return

    val inflateRawValue = (inflateObject as Number).toDouble()

    if (abs(inflateRawValue) < 1e-9) {
      this.cubeJsonMap.remove("inflate")

      return
    }

    val inflate: Float = this.roundToFourDecimalPlaces((inflateRawValue * BoneBlueprint.ARMOR_STAND_HEAD_SIZE_MULTIPLIER).toFloat())

    val newFrom: Vector3f = Vector3f(this.from).sub(inflate, inflate, inflate)
    val newTo: Vector3f = Vector3f(this.to).add(inflate, inflate, inflate)

    if (newFrom.x > newTo.x || newFrom.y > newTo.y || newFrom.z > newTo.z) {
      Logger.warn("Inflate on model '$this.modelName' is too negative and would invert the cube.")

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

    this.cubeJsonMap["from"] = listOf<Float?>(
      this.roundToFourDecimalPlaces(this.from.get(0)),
      this.roundToFourDecimalPlaces(this.from.get(1)),
      this.roundToFourDecimalPlaces(this.from.get(2))
    )

    this.cubeJsonMap["to"] = listOf<Float?>(
      this.roundToFourDecimalPlaces(this.to.get(0)),
      this.roundToFourDecimalPlaces(this.to.get(1)),
      this.roundToFourDecimalPlaces(this.to.get(2))
    )

    this.cubeJsonMap.remove("inflate")
  }
}
