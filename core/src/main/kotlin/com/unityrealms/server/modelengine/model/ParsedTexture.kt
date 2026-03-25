package com.unityrealms.server.modelengine.model

import com.unityrealms.server.modelengine.ModelEngine
import com.google.gson.GsonBuilder
import com.unityrealms.server.modelengine.model.resourcepack.ResourcePackConverter
import com.unityrealms.server.modelengine.logger.Logger

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO

/**
 * Represents a parsed texture in a model.
 *
 * @param modelName The name of the model to which the texture belongs.
 * @param imageIndex The index of the texture in the model.
 * @param textureMap The texture data map containing the image source.
 */
class ParsedTexture(modelName: String?, imageIndex: Int, textureMap: MutableMap<*, *>) {

  /**
   * Represents the animation metadata for an animated texture.
   */
  private class AnimationMetadata


  val isAnimated: Boolean
    get() = this.height > this.width && this.height % this.width == 0.0


  private var identifier: Int? = null


  private var fileName: String? = null

  private var imagePath: String? = null


  private var height = 0.0

  private var width = 0.0

  var textureHeight: Double = 0.0
    private set

  var textureWidth: Double = 0.0
    private set

  private var frameTime = 1.0


  private var isValid = false

  init {
    try {
      this.identifier = imageIndex

      this.fileName = ResourcePackConverter.convert(textureMap["name"] as String?)

      if (!(this.fileName!!.contains(".png"))) {
        if (!(this.fileName!!.contains("."))) {
          this.fileName += ".png"
        } else {
          this.fileName!!.split("\\.".toRegex()).dropLastWhile {
            it.isEmpty()
          }.toTypedArray()[0] += ".png"
        }
      }

      val imageFile = this.generateImageFile(modelName, textureMap)

      if (textureMap["height"] != null) {
        this.height = textureMap["height"] as Double
        this.width = textureMap["width"] as Double

        this.textureHeight = textureMap["uv_height"] as Double
        this.textureWidth = textureMap["uv_width"] as Double

        this.frameTime = textureMap["frame_time"] as Double
      } else {
        val bufferedImage: BufferedImage = ImageIO.read(imageFile)

        this.height = bufferedImage.height.toDouble()
        this.width = bufferedImage.width.toDouble()

        if (this.height > this.width && this.height % this.width == 0.0) {
          this.textureHeight = this.width
          this.textureWidth = this.width

          this.frameTime = 1.0

          Logger.information("Detected an animated texture: '${this.fileName}' (${(this.height / this.width)} frames).")
        } else {
          this.textureHeight = bufferedImage.height.toDouble()
          this.textureWidth = bufferedImage.width.toDouble()
        }
      }
    } catch (exception: Exception) {
      this.isValid = false

      throw RuntimeException("Failed to parse texture '${textureMap["name"]}': ${exception.message}")
    }

    if (this.isAnimated) {
      this.generateMCMetaFile()
    }

    this.isValid = true
  }

  /**
   * Generates the image file for the texture.
   *
   * @param modelName The name of the model to which the texture belongs.
   * @param textureMap The texture data map containing the image source.
   *
   * @return The generated image file.
   */
  private fun generateImageFile(modelName: String?, textureMap: MutableMap<*, *>): File {
    var base64Image = textureMap["source"] as String

    base64Image = base64Image.split(",".toRegex()).dropLastWhile {
      it.isEmpty()
    }.toTypedArray()[base64Image.split(",".toRegex()).dropLastWhile {
      it.isEmpty()
    }.toTypedArray().size - 1]

    val byteArrayInputStream = ByteArrayInputStream(Base64Coder.decodeLines(base64Image))

    this.imagePath = ModelEngine.javaPlugin.dataFolder.absolutePath +
      File.separatorChar + "output" +
      File.separatorChar + "ModelEngine" +
      File.separatorChar + "assets" +
      File.separatorChar + "modelengine" +
      File.separatorChar + "textures" +
      File.separatorChar + "entity" +
      File.separatorChar + modelName +
      File.separatorChar + this.fileName

    val imageFile = File(this.imagePath!!)

    try {
      imageFile.writeBytes(byteArrayInputStream.readAllBytes())
    } catch (ioException: IOException) {
      throw RuntimeException("Failed to write the image file: '${imageFile.absolutePath}': ${ioException.message}")
    }

    return imageFile
  }

  /**
   * Generates the .mcmeta file for an animated texture.
   */
  private fun generateMCMetaFile() {
    val mcMetaFile = File("${this.imagePath}.mcmeta")

    try {
      mcMetaFile.writeText(GsonBuilder().create().toJson(AnimationMetadata()), StandardCharsets.UTF_8)

      Logger.information("Created .mcmeta file for an animated texture: '${this.fileName}'.")
    } catch (ioException: IOException) {
      throw RuntimeException("Failed to write the .mcmeta file: '${mcMetaFile.absolutePath}': ${ioException.message}")
    }
  }
}
