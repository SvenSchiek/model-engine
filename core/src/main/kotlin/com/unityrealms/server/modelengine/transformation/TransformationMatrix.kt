package com.unityrealms.server.modelengine.transformation

import kotlin.math.*

import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Represents a transformation matrix.
 */
class TransformationMatrix {

  companion object {

    /**
     * Multiplies two transformation matrices and stores the result in a third matrix.
     *
     * @param firstTransformationMatrix The first transformation matrix.
     * @param secondTransformationMatrix The second transformation matrix.
     * @param resultTransformationMatrix The matrix to store the result.
     */
    @JvmStatic
    fun multiply(
      firstTransformationMatrix: TransformationMatrix,
      secondTransformationMatrix: TransformationMatrix,
      resultTransformationMatrix: TransformationMatrix
    ) {
      resultTransformationMatrix.replacementMatrix = Matrix4d(firstTransformationMatrix.replacementMatrix).mul(secondTransformationMatrix.replacementMatrix)

      for (i in 0..3) {
        for (j in 0..3) {
          resultTransformationMatrix.matrix[i]!![j] = 0.0

          for (k in 0..3) {
            resultTransformationMatrix.matrix[k]!![j] += firstTransformationMatrix.matrix[k]!![k] * secondTransformationMatrix.matrix[k]!![j]
          }
        }
      }
    }
  }

  var replacementMatrix: Matrix4d = Matrix4d()


  private var matrix = Array<DoubleArray?>(4) {
    DoubleArray(4)
  }

  init {
    this.resetToIdentityMatrix()
  }

  /**
   * Resets the matrix to the identity matrix.
   */
  fun resetToIdentityMatrix() {
    this.replacementMatrix.identity()

    for (i in 0..3) {
      for (j in 0..3) {
        this.matrix[i]!![j] = (if (i == j) 1 else 0).toDouble()
      }
    }
  }


  val rotation: DoubleArray
    get() {
      val rotation = DoubleArray(3)

      rotation[1] = atan2(-this.matrix[2]!![0], sqrt(this.matrix[0]!![0] * this.matrix[0]!![0] + this.matrix[1]!![0] * this.matrix[1]!![0]))

      if (abs(this.matrix[2]!![0]) < 1e-6 && abs(this.matrix[2]!![2]) < 1e-6) {
        rotation[0] = atan2(this.matrix[1]!![2], this.matrix[1]!![1])
        rotation[2] = 0.0
      } else {
        rotation[0] = atan2(this.matrix[2]!![1], this.matrix[2]!![2])
        rotation[2] = atan2(this.matrix[1]!![0], this.matrix[0]!![0])
      }

      return rotation
    }

  val scale: DoubleArray
    get() {
      val scale = DoubleArray(3)

      scale[0] = sqrt(
        this.matrix[0]!![0] * this.matrix[0]!![0] + this.matrix[1]!![0] * this.matrix[1]!![0] + this.matrix[2]!![0] * this.matrix[2]!![0]
      )

      scale[1] = sqrt(
        this.matrix[0]!![1] * this.matrix[0]!![1] + this.matrix[1]!![1] * this.matrix[1]!![1] + this.matrix[2]!![1] * this.matrix[2]!![1]
      )

      scale[2] = sqrt(
        this.matrix[0]!![2] * this.matrix[0]!![2] + this.matrix[1]!![2] * this.matrix[1]!![2] + this.matrix[2]!![2] * this.matrix[2]!![2]
      )

      this.replacementMatrix.getScale(Vector3d())

      return scale
    }

  val translation: DoubleArray
    get() = doubleArrayOf(this.matrix[0]!![3], this.matrix[1]!![3], this.matrix[2]!![3])


  /**
   * Scales the matrix by x, y, z coordinates.
   *
   * @param x The x scale factor.
   * @param y The y scale factor.
   * @param z The z scale factor.
   */
  fun scale(x: Double, y: Double, z: Double) {
    val transformationMatrix = TransformationMatrix()

    transformationMatrix.matrix[0]!![0] = x
    transformationMatrix.matrix[1]!![1] = y
    transformationMatrix.matrix[2]!![2] = z

    this.multiplyWith(transformationMatrix)
  }


  /**
   * Translates the matrix by x, y, z coordinates.
   *
   * @param x The x coordinate to translate by.
   * @param y The y coordinate to translate by.
   * @param z The z coordinate to translate by.
   */
  fun translateLocal(x: Float, y: Float, z: Float) {
    val transformationMatrix = TransformationMatrix()

    transformationMatrix.matrix[0]!![3] = x.toDouble()
    transformationMatrix.matrix[1]!![3] = y.toDouble()
    transformationMatrix.matrix[2]!![3] = z.toDouble()

    this.multiplyWith(transformationMatrix)

    this.replacementMatrix.translateLocal(Vector3d(x.toDouble(), y.toDouble(), z.toDouble()))
  }

  /**
   * Translates the matrix by a vector.
   *
   * @param vector The vector to translate by.
   */
  fun translateLocal(vector: Vector3f) {
    this.translateLocal(vector.get(0), vector.get(1), vector.get(2))
  }


  /**
   * Rotates the matrix by an angle in radians around the x-axis.
   *
   * @param angleRadians The angle in radians to rotate by.
   */
  fun rotateX(angleRadians: Double) {
    val transformationMatrix = TransformationMatrix()

    transformationMatrix.matrix[1]!![1] = cos(angleRadians)
    transformationMatrix.matrix[1]!![2] = -sin(angleRadians)
    transformationMatrix.matrix[2]!![1] = sin(angleRadians)
    transformationMatrix.matrix[2]!![2] = cos(angleRadians)

    this.multiplyWith(transformationMatrix)
  }

  /**
   * Rotates the matrix by an angle in radians around the y-axis.
   *
   * @param angleRadians The angle in radians to rotate by.
   */
  fun rotateY(angleRadians: Double) {
    val transformationMatrix = TransformationMatrix()

    transformationMatrix.matrix[0]!![0] = cos(angleRadians)
    transformationMatrix.matrix[0]!![2] = sin(angleRadians)
    transformationMatrix.matrix[2]!![0] = -sin(angleRadians)
    transformationMatrix.matrix[2]!![2] = cos(angleRadians)

    this.multiplyWith(transformationMatrix)
  }

  /**
   * Rotates the matrix by an angle in radians around the z-axis.
   *
   * @param angleRadians The angle in radians to rotate by.
   */
  fun rotateZ(angleRadians: Double) {
    val transformationMatrix = TransformationMatrix()

    transformationMatrix.matrix[0]!![0] = cos(angleRadians)
    transformationMatrix.matrix[0]!![1] = -sin(angleRadians)
    transformationMatrix.matrix[1]!![0] = sin(angleRadians)
    transformationMatrix.matrix[1]!![1] = cos(angleRadians)

    this.multiplyWith(transformationMatrix)
  }

  /**
   * Rotates the local matrix by x, y, z angles in radians.
   *
   * @param x The x angle in radians.
   * @param y The y angle in radians.
   * @param z The z angle in radians.
   */
  fun rotateLocal(x: Double, y: Double, z: Double) {
    this.rotateZ(z)
    this.rotateY(y)
    this.rotateX(x)

    this.replacementMatrix.rotateLocalZ(z)
    this.replacementMatrix.rotateLocalY(y)
    this.replacementMatrix.rotateLocalX(x)
  }

  /**
   * Rotates the animation matrix by x, y, z angles in radians.
   *
   * @param x The x angle in radians.
   * @param y The y angle in radians.
   * @param z The z angle in radians.
   */
  fun rotateAnimation(x: Double, y: Double, z: Double) {
    this.rotateZ(z)
    this.rotateY(y)
    this.rotateX(x)

    this.replacementMatrix.rotateLocalZ(z)
    this.replacementMatrix.rotateLocalY(y)
    this.replacementMatrix.rotateLocalX(x)
  }


  /**
   * Resets the rotation of the matrix to the identity rotation.
   */
  fun resetRotation() {
    val identityRotation = arrayOf<DoubleArray?>(
      doubleArrayOf(1.0, 0.0, 0.0, 0.0),
      doubleArrayOf(0.0, 1.0, 0.0, 0.0),
      doubleArrayOf(0.0, 0.0, 1.0, 0.0),
      doubleArrayOf(0.0, 0.0, 0.0, 1.0)
    )

    identityRotation[0]!![3] = this.matrix[0]!![3]
    identityRotation[1]!![3] = this.matrix[1]!![3]
    identityRotation[2]!![3] = this.matrix[2]!![3]

    for (i in 0..2) {
      System.arraycopy(identityRotation[i]!!, 0, this.matrix[i]!!, 0, 3)
    }

    this.replacementMatrix.m00(1.0).m01(0.0).m02(0.0)
    this.replacementMatrix.m10(0.0).m11(1.0).m12(0.0)
    this.replacementMatrix.m20(0.0).m21(0.0).m22(1.0)
  }


  /**
   * Multiplies this transformation matrix with another one.
   *
   * @param otherTransformationMatrix The other transformation matrix to multiply with.
   */
  private fun multiplyWith(otherTransformationMatrix: TransformationMatrix) {
    val resultMatrix = Array<DoubleArray?>(4) {
      DoubleArray(4)
    }

    for (i in 0..3) {
      for (j in 0..3) {
        for (k in 0..3) {
          resultMatrix[i]!![j] += this.matrix[i]!![k] * otherTransformationMatrix.matrix[k]!![j]
        }
      }
    }

    this.matrix = resultMatrix
  }
}
