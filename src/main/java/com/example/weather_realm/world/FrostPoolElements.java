package com.example.weather_realm.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

/**
 * Builds a copy of a pool element whose processor list also contains the supplied processors.
 * The extra processors are baked into the element so they survive structure serialization.
 */
public final class FrostPoolElements {
    private FrostPoolElements() {
    }

    public static StructurePoolElement withProcessors(StructurePoolElement element, StructureProcessor... processors) {
        if (processors.length == 0 || !(element instanceof SinglePoolElement)) {
            return element;
        }
        DataResult<JsonElement> encoded = StructurePoolElement.CODEC.encodeStart(JsonOps.INSTANCE, element);
        JsonElement json = encoded.result().orElse(null);
        if (json == null || !json.isJsonObject()) {
            return element;
        }
        JsonObject object = json.getAsJsonObject();
        JsonArray processorArray = extractProcessors(object.get("processors"));
        for (StructureProcessor processor : processors) {
            StructureProcessorType.SINGLE_CODEC.encodeStart(JsonOps.INSTANCE, processor)
                    .result()
                    .ifPresent(processorArray::add);
        }
        JsonObject wrapper = new JsonObject();
        wrapper.add("processors", processorArray);
        object.add("processors", wrapper);
        return StructurePoolElement.CODEC.parse(JsonOps.INSTANCE, object).result().orElse(element);
    }

    private static JsonArray extractProcessors(JsonElement element) {
        JsonArray array = new JsonArray();
        if (element == null) {
            return array;
        }
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(array::add);
        } else if (element.isJsonObject()) {
            JsonElement inner = element.getAsJsonObject().get("processors");
            if (inner != null && inner.isJsonArray()) {
                inner.getAsJsonArray().forEach(array::add);
            }
        }
        return array;
    }
}
