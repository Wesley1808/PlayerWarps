package me.wesley1808.playerwarps.config;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.lang.reflect.Type;

public class Json {
    private static final HolderLookup.Provider PROVIDER = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    public static final Gson CONFIG = new GsonBuilder()
            .registerTypeHierarchyAdapter(ResourceLocation.class, new CodecSerializer<>(ResourceLocation.CODEC))
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static final Gson PLAYER_WARPS = new GsonBuilder()
            .registerTypeHierarchyAdapter(BlockPos.class, new BlockPosSerializer())
            .registerTypeHierarchyAdapter(ResourceLocation.class, new CodecSerializer<>(ResourceLocation.CODEC))
            .registerTypeHierarchyAdapter(Item.class, new RegistrySerializer<>(BuiltInRegistries.ITEM))
            .excludeFieldsWithoutExposeAnnotation()
            .disableHtmlEscaping()
            .create();

    private static class BlockPosSerializer implements JsonDeserializer<BlockPos>, JsonSerializer<BlockPos> {
        @Override
        public BlockPos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonArray()) {
                JsonArray array = json.getAsJsonArray();
                return new BlockPos(
                        array.get(0).getAsInt(),
                        array.get(1).getAsInt(),
                        array.get(2).getAsInt()
                );
            }
            return null;
        }

        @Override
        public JsonElement serialize(BlockPos pos, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray array = new JsonArray();
            array.add(pos.getX());
            array.add(pos.getY());
            array.add(pos.getZ());
            return array;
        }
    }

    private record RegistrySerializer<T>(Registry<T> registry) implements JsonSerializer<T>, JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return this.registry.getValue(ResourceLocation.tryParse(json.getAsString()));
            }
            return null;
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(String.valueOf(this.registry.getKey(src)));
        }
    }

    private record CodecSerializer<T>(Codec<T> codec) implements JsonSerializer<T>, JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return this.codec.decode(PROVIDER.createSerializationContext(JsonOps.INSTANCE), element).getOrThrow().getFirst();
            } catch (Throwable throwable) {
                return null;
            }
        }

        @Override
        public JsonElement serialize(T value, Type typeOfSrc, JsonSerializationContext context) {
            if (value == null) {
                return JsonNull.INSTANCE;
            }

            try {
                return this.codec.encodeStart(PROVIDER.createSerializationContext(JsonOps.INSTANCE), value).getOrThrow();
            } catch (Throwable throwable) {
                return JsonNull.INSTANCE;
            }
        }
    }
}
