package com.magmaguy.freeminecraftmodels.dataconverter;

import com.unityrealms.server.modelengine.model.skeleton.SkeletonBlueprint;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;

public class AnimationsBlueprint {
    @Getter
    private final HashMap<String, AnimationBlueprint> animations = new HashMap<>();

    public AnimationsBlueprint(List<Object> rawAnimationData, String modelName, SkeletonBlueprint skeletonBlueprint, int blockBenchVersion) {
        for (Object animation : rawAnimationData) {
            AnimationBlueprint animationBlueprintObject = new AnimationBlueprint(animation, modelName, skeletonBlueprint, blockBenchVersion);
            animations.put(animationBlueprintObject.getAnimationName(), animationBlueprintObject);
        }
    }
}
