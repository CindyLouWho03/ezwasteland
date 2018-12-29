package com.ezrol.terry.minecraft.wastelands.mixin;

import com.ezrol.terry.minecraft.wastelands.EzwastelandsFabric;
import com.ezrol.terry.minecraft.wastelands.api.RegionCore;
import com.ezrol.terry.minecraft.wastelands.world.WastelandChunkGenerator;
import com.ezrol.terry.minecraft.wastelands.world.WastelandChunkGeneratorSettings;
import com.ezrol.terry.minecraft.wastelands.world.WastelandsBiomeSource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.JsonOps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.datafixers.NbtOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeSourceType;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSourceConfig;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.OverworldDimension;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorSettings;
import net.minecraft.world.level.LevelGeneratorType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OverworldDimension.class)
public abstract class CommonMixinOverworldDimension extends Dimension {
    public CommonMixinOverworldDimension(World world_1, DimensionType dimensionType_1) {
        super(world_1, dimensionType_1);
    }

    private static final Logger LOGGER = LogManager.getLogger("ChunkGenType");

    @Inject(at = @At(value="INVOKE",
            target="Lnet/minecraft/world/gen/chunk/ChunkGeneratorType;createSettings()Lnet/minecraft/world/gen/chunk/ChunkGeneratorSettings;"), method = "createChunkGenerator", cancellable = true)
    public void createChunkGenerator(CallbackInfoReturnable ci){
        LevelGeneratorType type = this.world.getLevelProperties().getGeneratorType();
        ChunkGeneratorType<WastelandChunkGeneratorSettings, WastelandChunkGenerator> ezwastelands = EzwastelandsFabric.WASTELANDS;

        if(type == EzwastelandsFabric.WASTELANDS_LEVEL_TYPE){
            LOGGER.info("Use wastelands generator");
            CompoundTag opts = this.world.getLevelProperties().getGeneratorOptions();
            WastelandChunkGeneratorSettings settings = new WastelandChunkGeneratorSettings(opts);
            VanillaLayeredBiomeSourceConfig biomeSrcCfg = ((VanillaLayeredBiomeSourceConfig)BiomeSourceType.VANILLA_LAYERED.getConfig()).setGeneratorSettings(new OverworldChunkGeneratorSettings()).setLevelProperties(this.world.getLevelProperties());

            settings.initBuffet(false);
            //noinspection unchecked
            ci.setReturnValue(
                    ezwastelands.create(this.world, new WastelandsBiomeSource(biomeSrcCfg, settings),settings)
            );
        }
        if(type == LevelGeneratorType.BUFFET){
            //likely we don't care, but see if wastelands is selected
            JsonElement rootElement = (JsonElement) Dynamic.convert(NbtOps.INSTANCE, JsonOps.INSTANCE, this.world.getLevelProperties().getGeneratorOptions());
            JsonObject rootObj = rootElement.getAsJsonObject();
            String generator = "unknown";
            LOGGER.info("testing:  " + rootObj.toString());
            try {
                generator = rootObj.getAsJsonObject("chunk_generator").getAsJsonPrimitive("type").getAsString();
            } catch (Exception e){
                return;
            }
            if(generator.equals("ezwastelands:wastelands")){
                //we are up use the "BUFFET" default
                String json = "{\"terrainvariation\":{\"amplification\":30,\"variation\":1},\"randopts\":{\"oceans\":false,\"oceanblock\":\"minecraft:water\",\"globaloffset\":0,\"villages\":false,\"villagechance\":10.0,\"strongholds\":true},\"domes\":{\"lgmincount\":3,\"lgmaxcount\":4,\"lgradius\":34,\"lgheight\":7,\"midmincount\":2,\"midmaxcount\":3,\"midradius\":23,\"midheight\":8,\"smmincount\":1,\"smmaxcount\":2,\"smradius\":18,\"smheight\":9},\"shallows\":{\"mincount\":4,\"maxcount\":5,\"radius\":42,\"depth\":5},\"spire\":{\"count\":2,\"size\":6}}";
                CompoundTag tags = WastelandChunkGeneratorSettings.CoreConfigToCompound(
                        new RegionCore(json,null,null));
                WastelandChunkGeneratorSettings settings = new WastelandChunkGeneratorSettings(tags);
                settings.initBuffet(true);

                //get the selected biome
                Biome singlebiome = Biomes.DEEP_COLD_OCEAN;
                try{
                    String biomename = rootObj.getAsJsonObject("biome_source")
                            .getAsJsonObject()
                            .getAsJsonObject("options")
                            .getAsJsonArray("biomes")
                            .get(0).getAsString();
                    singlebiome = Registry.BIOME.get(new Identifier(biomename));
                } catch (Exception e){
                    LOGGER.error("Unable to parse biome from Buffet typo");
                }
                FixedBiomeSourceConfig fixedCfg = new FixedBiomeSourceConfig();
                fixedCfg.method_8782(singlebiome);

                //noinspection unchecked
                ci.setReturnValue(
                        ezwastelands.create(this.world, new FixedBiomeSource(fixedCfg),settings)
                );
            }
        }
    }

    //custom spawn logic
    @Inject(at = @At("HEAD"), method = "method_12444", cancellable = true)
    public void playerSpawn(int blockx, int blockz, boolean force,CallbackInfoReturnable ci){
        LevelGeneratorType type = this.world.getLevelProperties().getGeneratorType();
        if(type == EzwastelandsFabric.WASTELANDS_LEVEL_TYPE){
            //correct a spawn location
            WastelandChunkGenerator gen = (WastelandChunkGenerator) world.getChunkManager().getChunkGenerator();
            //noinspection unchecked
            ci.setReturnValue(gen.verifySpawn(blockx, blockz, force));
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float method_12455() {
        LevelGeneratorType type = this.world.getLevelProperties().getGeneratorType();

        if(type == EzwastelandsFabric.WASTELANDS_LEVEL_TYPE){
            return 200.0f;
        }
        return super.method_12455();
    }
}
