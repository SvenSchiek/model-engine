package com.unityrealms.server.modelengine.model.skeleton

import com.unityrealms.server.modelengine.model.skeleton.bone.BoneBlueprint
import com.magmaguy.freeminecraftmodels.dataconverter.HitboxBlueprint
import com.magmaguy.freeminecraftmodels.dataconverter.IKChainBlueprint
import com.magmaguy.freeminecraftmodels.dataconverter.LocatorBlueprint
import com.magmaguy.freeminecraftmodels.dataconverter.NullObjectBlueprint
import com.unityrealms.server.modelengine.model.ParsedTexture
import java.util.ArrayList
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

class SkeletonBlueprint(
  parsedTextures: MutableList<ParsedTexture?>?,
  outlinerJSON: MutableList<*>,
  values: HashMap<String?, Any?>?,
  locators: HashMap<String?, MutableMap<String?, Any?>?>?,
  nullObjects: HashMap<String?, MutableMap<String?, Any?>?>?,
  textureReferences: MutableMap<String?, MutableMap<String?, Any?>?>?,
  modelName: String?,
  pathName: String?,
  resolutionWidth: Double,
  resolutionHeight: Double
) {
  //In BlockBench models are referred to by name for animations, and names are unique
  val boneMap: HashMap<String?, BoneBlueprint> = HashMap<String?, BoneBlueprint>()

  //Map of bone UUIDs to bone blueprints (for IK chain lookup)
  val boneByUniqueIdentifierMap: HashMap<String?, BoneBlueprint?> = HashMap<String?, BoneBlueprint?>()

  //Map of locator UUIDs to locator blueprints
  private val locatorMap = HashMap<String?, LocatorBlueprint?>()

  //Map of null object UUIDs to null object blueprints
  private val nullObjectMap = HashMap<String?, NullObjectBlueprint>()

  //List of all IK chains in this model
  private val ikChains: MutableList<IKChainBlueprint> = ArrayList<IKChainBlueprint>()

  @Getter
  private val mountPointBlueprints: MutableList<BoneBlueprint?> =
    ArrayList<BoneBlueprint?>()

  @Getter
  private val mainModel: MutableList<BoneBlueprint?> = ArrayList<BoneBlueprint?>()

  @Getter
  private var modelName: String? = "Default Name"

  @Getter
  private var hitbox: HitboxBlueprint? = null

  init {
    this.modelName = modelName

    //Create a root bone for everything
    val rootBone: BoneBlueprint = BoneBlueprint(modelName, this, null)
    val rootChildren: MutableList<BoneBlueprint?> = ArrayList<BoneBlueprint?>()

    for (i in outlinerJSON.indices) {
      if (outlinerJSON.get(i) !is MutableMap<*, *>) continue
      val bone = outlinerJSON.get(i) as MutableMap<String?, Any?>
      if ((bone.get("name") as String).equals("hitbox", ignoreCase = true)) hitbox =
        HitboxBlueprint(bone, values, modelName, null)
      else {
        val boneBlueprint: BoneBlueprint = BoneBlueprint(
          modelName,
          this,
          rootBone,
          resolutionHeight,
          resolutionWidth,
          parsedTextures,
          textureReferences,
          bone,
          values,
          locators,
          nullObjects
        )
        rootChildren.add(boneBlueprint)
        if (boneBlueprint.getMetaBone() != null) rootChildren.add(boneBlueprint.getMetaBone())
      }
    }

    rootBone.setBoneBlueprintChildren(rootChildren)
    mainModel.add(rootBone)

    // Build IK chains after all bones, locators, and null objects are parsed
    buildIKChains()

    // Collect mount point bone blueprints
    for (bone in boneMap.values) {
      if (bone.isMountPoint()) mountPointBlueprints.add(bone)
    }
  }

  /**
   * Builds IK chains from null objects that have valid IK configuration.
   * For each null object with ik_source and ik_target, finds the bone chain
   * by walking UP from target to source, then reverses for root-to-tip order.
   */
  private fun buildIKChains() {
    for (nullObj in nullObjectMap.values) {
      if (!nullObj.hasValidIKConfig()) {
        continue
      }

      // Resolve ik_source to a bone
      val sourceBone: BoneBlueprint? = boneByUniqueIdentifierMap.get(nullObj.getIkSourceUUID())
      if (sourceBone == null) {
        Logger.warn("IK chain in model " + modelName + ": Could not find source bone with UUID " + nullObj.getIkSourceUUID())
        continue
      }
      nullObj.setIkSourceBone(sourceBone)

      // Resolve ik_target - could be a locator or a bone
      val targetLocator = locatorMap.get(nullObj.getIkTargetUUID())
      val targetBone: BoneBlueprint? = boneByUniqueIdentifierMap.get(nullObj.getIkTargetUUID())

      if (targetLocator == null && targetBone == null) {
        Logger.warn("IK chain in model " + modelName + ": Could not find target with UUID " + nullObj.getIkTargetUUID())
        continue
      }

      // Find the chain by walking from target to source
      val chainBones: MutableList<BoneBlueprint?>
      if (targetLocator != null) {
        nullObj.setIkTargetLocator(targetLocator)
        // Start from the locator's parent bone
        val startBone: BoneBlueprint? = targetLocator.getParentBone()
        chainBones = findChainBones(sourceBone, startBone, null)
      } else {
        nullObj.setIkTargetBone(targetBone)
        // Start from the target bone's parent (we don't include the target bone in the chain)
        val startBone: BoneBlueprint? = targetBone.getParent()
        chainBones = findChainBones(sourceBone, startBone, targetBone)
      }

      if (chainBones.isEmpty()) {
        Logger.warn("IK chain in model " + modelName + ": Could not find path from source to target")
        continue
      }

      // Create the IK chain blueprint
      val chain: IKChainBlueprint?
      if (targetLocator != null) {
        chain = IKChainBlueprint(chainBones, targetLocator, nullObj)
      } else {
        chain = IKChainBlueprint(chainBones, targetBone, nullObj)
      }

      ikChains.add(chain!!)
    }
  }

  /**
   * Finds the bone chain by walking UP from the target bone to the source bone.
   * The resulting list is reversed to get root-to-tip order.
   * If walking up fails (e.g., sibling bones), tries alternative methods.
   *
   * @param sourceBone       The root bone of the IK chain (ik_source)
   * @param startBone        The bone to start from (target's parent)
   * @param actualTargetBone The actual target bone (for sibling detection), can be null
   * @return List of bones from source to tip, or empty list if no path found
   */
  private fun findChainBones(
    sourceBone: BoneBlueprint?,
    startBone: BoneBlueprint?,
    actualTargetBone: BoneBlueprint?
  ): MutableList<BoneBlueprint?> {
    var chain: MutableList<BoneBlueprint?> = ArrayList<BoneBlueprint?>()

    // First check if source and actual target are siblings (common case for flat IK setups)
    if (actualTargetBone != null && sourceBone != null && sourceBone.getParent() === actualTargetBone.getParent()) {
      return findSiblingChain(sourceBone, actualTargetBone)
    }

    if (startBone == null) {
      // For sibling bones or when target is a bone without a meaningful parent,
      // try to find chain by walking DOWN from source
      return findChainBonesDownward(sourceBone, actualTargetBone)
    }

    // Walk up from startBone to sourceBone
    var current: BoneBlueprint? = startBone
    var steps = 0
    while (current != null) {
      chain.add(current)
      if (current === sourceBone) {
        // Found the source, reverse and return
        Collections.reverse(chain)
        return chain
      }
      current = current.getParent()
      steps++
      if (steps > 100) {
        break
      }
    }

    // Didn't find source in the parent chain
    // Try walking DOWN from source to find the target
    val searchTarget: BoneBlueprint = if (actualTargetBone != null) actualTargetBone else startBone
    chain = findChainBonesDownward(sourceBone, searchTarget)
    if (!chain.isEmpty()) {
      return chain
    }

    return ArrayList<BoneBlueprint?>()
  }

  /**
   * Finds a chain by walking DOWN from source through children to find target.
   *
   * @param sourceBone The source bone to start from
   * @param targetBone The target bone to find (or null to just return source's descendants)
   * @return List of bones from source to target, or empty list if not found
   */
  private fun findChainBonesDownward(
    sourceBone: BoneBlueprint?,
    targetBone: BoneBlueprint?
  ): MutableList<BoneBlueprint?> {
    if (sourceBone == null) {
      return ArrayList<BoneBlueprint?>()
    }

    val chain: MutableList<BoneBlueprint?> = ArrayList<BoneBlueprint?>()
    chain.add(sourceBone)

    if (targetBone == null) {
      // No specific target, just return the source as a single-bone chain
      return chain
    }

    // BFS to find path from source to target through children
    if (findPathToTarget(sourceBone, targetBone, chain)) {
      return chain
    }

    return ArrayList<BoneBlueprint?>()
  }

  /**
   * Recursively finds a path from current bone to target through children.
   */
  private fun findPathToTarget(
    current: BoneBlueprint,
    target: BoneBlueprint?,
    path: MutableList<BoneBlueprint?>
  ): Boolean {
    for (child in current.getBoneBlueprintChildren()) {
      path.add(child)
      if (child === target) {
        return true
      }
      if (findPathToTarget(child, target, path)) {
        return true
      }
      path.removeAt(path.size - 1)
    }
    return false
  }

  /**
   * Creates a chain from sibling bones by finding bones between source and target
   * in their parent's children list.
   *
   * @param sourceBone The source bone
   * @param targetBone The target bone (or its parent for locator targets)
   * @return List of bones forming the chain
   */
  private fun findSiblingChain(
    sourceBone: BoneBlueprint,
    targetBone: BoneBlueprint?
  ): MutableList<BoneBlueprint?> {
    val chain: MutableList<BoneBlueprint?> = ArrayList<BoneBlueprint?>()

    val parent: BoneBlueprint? = sourceBone.getParent()
    if (parent == null) {
      // Both are at root level - check mainModel
      // For root level siblings, we can only reliably include source
      // (we don't know the order or which bones are between)
      chain.add(sourceBone)
      return chain
    }

    // Find indices of source and target in parent's children
    val siblings: MutableList<BoneBlueprint?> = parent.getBoneBlueprintChildren()
    var sourceIndex = -1
    var targetIndex = -1

    for (i in siblings.indices) {
      if (siblings.get(i) === sourceBone) sourceIndex = i
      if (siblings.get(i) === targetBone) targetIndex = i
    }

    if (sourceIndex >= 0 && targetIndex >= 0) {
      // Add all bones between source and target (inclusive of source, exclusive of target)
      val start = min(sourceIndex, targetIndex)
      val end = max(sourceIndex, targetIndex)
      for (i in start..<end) {
        chain.add(siblings.get(i))
      }
      // Reverse if needed so source is first
      if (sourceIndex > targetIndex) {
        Collections.reverse(chain)
      }
    } else {
      // Fallback: just use source
      chain.add(sourceBone)
    }

    return chain
  }

  /**
   * Gets an IK chain by its controller's name.
   *
   * @param controllerName The name of the null object controller
   * @return The IK chain, or null if not found
   */
  fun getIKChainByControllerName(controllerName: String?): IKChainBlueprint? {
    for (chain in ikChains) {
      if (chain.getController().getName().equals(controllerName)) {
        return chain
      }
    }
    return null
  }
}