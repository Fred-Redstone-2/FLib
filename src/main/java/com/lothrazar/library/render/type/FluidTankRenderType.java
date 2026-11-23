package com.lothrazar.library.render.type;

import com.lothrazar.library.FutureLibMod;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * Source from MIT open source https://github.com/mekanism/Mekanism/tree/1.15x
 * <p>
 * https://github.com/mekanism/Mekanism/blob/1.15x/LICENSE
 *
 * Reuses the world text pipeline shape (translucent, not true alpha-cutout), matching the
 * simplification already used for FakeBlockRenderTypes.FAKE_BLOCK.
 */
public class FluidTankRenderType {

  private static final RenderPipeline RESIZABLE_PIPELINE = RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
      .withLocation(Identifier.fromNamespaceAndPath(FutureLibMod.MODID, "pipeline/fluidtank_resizable"))
      .withVertexShader("core/text")
      .withFragmentShader("core/text")
      .build();

  public static final RenderType RESIZABLE = RenderType.create(FutureLibMod.MODID + ":resizable_cuboid",
      RenderSetup.builder(RESIZABLE_PIPELINE)
          .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
          .useLightmap()
          .createRenderSetup());

  public static void registerPipelines(RegisterRenderPipelinesEvent event) {
    event.registerPipeline(RESIZABLE_PIPELINE);
  }
}
