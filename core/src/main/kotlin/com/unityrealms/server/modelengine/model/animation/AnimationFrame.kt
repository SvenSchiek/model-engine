package com.unityrealms.server.modelengine.model.animation

/**
 * Represents a single frame of an animation.
 */
class AnimationFrame {

  @JvmField
  var rotationX: Float = 0.0F

  @JvmField
  var rotationY: Float = 0.0F

  @JvmField
  var rotationZ: Float = 0.0F


  @JvmField
  var positionX: Float = 0.0F

  @JvmField
  var positionY: Float = 0.0F

  @JvmField
  var positionZ: Float = 0.0F


  @JvmField
  var scaleX: Float? = null

  @JvmField
  var scaleY: Float? = null

  @JvmField
  var scaleZ: Float? = null
}
