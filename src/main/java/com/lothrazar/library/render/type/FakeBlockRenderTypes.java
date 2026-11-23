package com.lothrazar.library.render.type;

import java.util.Optional;
import com.lothrazar.library.FutureLibMod;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.*;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import static com.mojang.renderpearl.api.pipeline.CompareOp.ALWAYS_PASS;
import static com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS;

/**
 * Render Types with help from direwolf20 MIT open source project https://github.com/Direwolf20-MC/BuildingGadgets/blob/1.15/LICENSE.md
 *
 * 26.1 port: see LaserRenderType for the general RenderPipeline/RenderSetup migration notes.
 * The old ADDITIVE_TRANSPARENCY shard is now just BlendFunction.ADDITIVE. The old distinction
 * between COLOR_WRITE (no depth write) and COLOR_DEPTH_WRITE (write depth without testing it) is
 * expressed via DepthStencilState: Optional.empty() for no depth attachment at all versus
 * new DepthStencilState(CompareOp.ALWAYS_PASS, true) for "always pass the test but still write".
 */
public class FakeBlockRenderTypes {

  public static final Identifier BEAM = Identifier.fromNamespaceAndPath(FutureLibMod.MODID, "textures/effect/beam.png");

  private static final RenderPipeline BEAM_PIPELINE = RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withLocation(Identifier.fromNamespaceAndPath(FutureLibMod.MODID, "pipeline/fakeblock_beam"))
      .withVertexShader("core/rendertype_text")
      .withFragmentShader("core/rendertype_text")
      .withCull(false)
      .withDepthStencilState(Optional.empty())
      .build();

  public static final RenderType LASER_MAIN_BEAM = RenderType.create("flib:mininglasermainbeam",
      RenderSetup.builder(BEAM_PIPELINE)
          .withTexture("Sampler0", BEAM)
          .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
          .createRenderSetup());

  private static final RenderPipeline FAKE_BLOCK_PIPELINE = RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withLocation(Identifier.fromNamespaceAndPath(FutureLibMod.MODID, "pipeline/fakeblock"))
      .withVertexShader("core/rendertype_text")
      .withFragmentShader("core/rendertype_text")
      .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
      .withDepthStencilState(new DepthStencilState(ALWAYS_PASS, true))
      .build();

  /**
   * used by TESR that render blocks with textures Shape builder, ghostsoundmuffler, render light camo.
   */
  public static final RenderType FAKE_BLOCK = RenderType.create("flib:fakeblock",
      RenderSetup.builder(FAKE_BLOCK_PIPELINE)
          .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
          .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
          .createRenderSetup());

  private static final RenderPipeline SOLID_COLOUR_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
      .withLocation(Identifier.fromNamespaceAndPath(FutureLibMod.MODID, "pipeline/solid_colour"))
      .withVertexShader("core/position_color")
      .withFragmentShader("core/position_color")
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(QUADS)
      .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
      .withDepthStencilState(new DepthStencilState(ALWAYS_PASS, true))
      // Unlike this file's other pipelines (BEAM/FAKE_BLOCK/LASER all disable culling), this one
      // never set an explicit cull state. renderCube()'s 6 hand-authored quad faces are only
      // correctly wound for some of the 6 faces, so with default backface culling active, half the
      // cube gets culled depending on view angle - showing as disconnected slabs instead of one
      // solid box. Disabling culling here is the low-risk fix instead of re-winding every face.
      .withCull(false)
      .build();

  /**
   * used by EventRender -> RenderWorldLastEvent by most held items that pick locations, such as cyclic:location_data
   * Render targets are selected by the render phase rather than set on the RenderType.
   */
  public static final RenderType TRANSPARENT_COLOUR = RenderType.create("flib:transparentcolour",
      RenderSetup.builder(SOLID_COLOUR_PIPELINE)
          .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
          .createRenderSetup());

  /**
   * used by most blocks that select blocks such as cyclic:forester, cyclic:harvester, cyclic:miner in TESRs
   */
  public static final RenderType SOLID_COLOUR = RenderType.create("flib:solidcolour",
      RenderSetup.builder(SOLID_COLOUR_PIPELINE)
          .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
          .createRenderSetup());

  // Like RenderPipelines.LINES_SNIPPET, but with depth testing disabled so the outline is always visible.
  private static final RenderPipeline NO_DEPTH_LINES_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
      .withLocation(Identifier.fromNamespaceAndPath(FutureLibMod.MODID, "pipeline/tomb_lines"))
      .withDepthStencilState(new DepthStencilState(ALWAYS_PASS, true))
      .build();

  /**
   * Used by cyclic:prospector
   */
  public static final RenderType TOMB_LINES = RenderType.create("flib:tomb_lines",
      RenderSetup.builder(NO_DEPTH_LINES_PIPELINE)
          .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
          .createRenderSetup());

  public static void registerPipelines(RegisterRenderPipelinesEvent event) {
    event.registerPipeline(BEAM_PIPELINE);
    event.registerPipeline(FAKE_BLOCK_PIPELINE);
    event.registerPipeline(SOLID_COLOUR_PIPELINE);
    event.registerPipeline(NO_DEPTH_LINES_PIPELINE);
  }
}
