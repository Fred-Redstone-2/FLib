package com.lothrazar.library.render.type;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

class RenderPipelineSamplersTest {

  @Test
  void worldTextPipelinesHaveUniqueBindingsAndVertexFormat() {
    assertAll(
        () -> assertWorldTextPipeline(FakeBlockRenderTypes.LASER_MAIN_BEAM),
        () -> assertWorldTextPipeline(FakeBlockRenderTypes.FAKE_BLOCK),
        () -> assertWorldTextPipeline(FluidTankRenderType.RESIZABLE),
        () -> assertWorldTextPipeline(LaserRenderType.LASER_MAIN_BEAM),
        () -> assertWorldTextPipeline(LaserRenderType.LASER_MAIN_ADDITIVE),
        () -> assertWorldTextPipeline(LaserRenderType.LASER_MAIN_CORE),
        () -> assertWorldTextPipeline(OverlayRenderType.overlayRenderer("flib:test_overlay",
            Identifier.fromNamespaceAndPath("flib", "textures/test_overlay.png"))));
  }

  @Test
  void everyWorldTextSetupUsesLightmap() {
    assertAll(
        () -> assertUsesLightmap(FakeBlockRenderTypes.LASER_MAIN_BEAM),
        () -> assertUsesLightmap(FakeBlockRenderTypes.FAKE_BLOCK),
        () -> assertUsesLightmap(FluidTankRenderType.RESIZABLE),
        () -> assertUsesLightmap(LaserRenderType.LASER_MAIN_BEAM),
        () -> assertUsesLightmap(LaserRenderType.LASER_MAIN_ADDITIVE),
        () -> assertUsesLightmap(LaserRenderType.LASER_MAIN_CORE),
        () -> assertUsesLightmap(OverlayRenderType.overlayRenderer("flib:test_overlay",
            Identifier.fromNamespaceAndPath("flib", "textures/test_overlay.png"))));
  }

  private static void assertWorldTextPipeline(RenderType renderType) {
    List<BindGroupLayout> layouts = renderType.pipeline().getBindGroupLayouts();
    assertEquals(List.of("Sampler0", "Sampler2"), BindGroupLayout.flattenSamplers(layouts), renderType.toString());

    List<String> uniformNames = BindGroupLayout.flattenUniforms(layouts).stream()
        .map(BindGroupLayout.UniformDescription::name)
        .toList();
    assertEquals(uniformNames.size(), uniformNames.stream().distinct().count(), renderType.toString());
    assertEquals(DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR,
        renderType.pipeline().getVertexFormatBinding(0), renderType.toString());
  }

  private static void assertUsesLightmap(RenderType renderType) throws ReflectiveOperationException {
    Field stateField = RenderType.class.getDeclaredField("state");
    stateField.setAccessible(true);
    Object state = stateField.get(renderType);
    Field useLightmapField = state.getClass().getDeclaredField("useLightmap");
    useLightmapField.setAccessible(true);
    assertTrue(useLightmapField.getBoolean(state), renderType.toString());
  }
}