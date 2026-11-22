package com.lothrazar.library.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FacadeUtil {

  public static void renderBlockState(Level level, BlockPos pos, SubmitNodeCollector collector, PoseStack matrixStack, BlockState facadeState, int packedLight, int packedOverlay) {
    if (facadeState == null) {
      return;
    }
    Minecraft mc = Minecraft.getInstance();
    BlockStateModelSet modelSet = mc.getModelManager().getBlockStateModelSet();
    BlockStateModel model = modelSet.get(facadeState);
    BlockAndTintGetter tintGetter = level instanceof BlockAndTintGetter batg ? batg : BlockAndTintGetter.EMPTY;
    BlockColors blockColors = mc.getBlockColors();
    RandomSource random = level.getRandom();
    List<BlockStateModelPart> parts = new ArrayList<>();
    model.collectParts(tintGetter, pos, facadeState, random, parts);
    QuadInstance instance = new QuadInstance();
    instance.setLightCoords(packedLight);
    instance.setOverlayCoords(packedOverlay);
    PoseStack.Pose pose = matrixStack.last();
    PoseStack ps = new PoseStack();
    ps.last().set(pose);
    for (BlockStateModelPart part : parts) {
      for (Direction direction : Direction.values()) {
        putQuads(tintGetter, pos, facadeState, blockColors, collector, ps, instance, part.getQuads(direction));
      }
      putQuads(tintGetter, pos, facadeState, blockColors, collector, ps, instance, part.getQuads(null));
    }
  }

  private record ColoredQuad(BakedQuad quad, int color) {}

  private static void putQuads(BlockAndTintGetter level, BlockPos pos, BlockState state, BlockColors blockColors,
                               SubmitNodeCollector collector, PoseStack poseStack, QuadInstance instance, List<BakedQuad> quads) {

    // 1) Calcul immédiat des couleurs, groupé par RenderType (un seul submit par type)
    Map<RenderType, List<ColoredQuad>> byType = new LinkedHashMap<>();
    for (BakedQuad quad : quads) {
      int tintIndex = quad.materialInfo().tintIndex();
      int color = -1;
      if (tintIndex != -1) {
        BlockTintSource tintSource = blockColors.getTintSource(state, tintIndex);
        if (tintSource != null) {
          color = tintSource.colorInWorld(state, level, pos);
        }
      }
      byType.computeIfAbsent(renderTypeForLayer(quad.materialInfo().layer()), k -> new ArrayList<>())
              .add(new ColoredQuad(quad, color));
    }

    // 2) Soumission différée : l'écriture dans le VertexConsumer se fera plus tard
    for (Map.Entry<RenderType, List<ColoredQuad>> entry : byType.entrySet()) {
      List<ColoredQuad> group = entry.getValue();
      collector.submitCustomGeometry(poseStack, entry.getKey(), (pose, vertexConsumer) -> {
        for (ColoredQuad cq : group) {
          instance.setColor(cq.color());
          vertexConsumer.putBakedQuad(pose, cq.quad(), instance);
        }
      });
    }
  }

//  private static void putQuads(BlockAndTintGetter level, BlockPos pos, BlockState state, BlockColors blockColors, SubmitNodeCollector ibuffer, PoseStack.Pose pose,
//      QuadInstance instance, List<BakedQuad> quads) {
//    for (BakedQuad quad : quads) {
//      int tintIndex = quad.materialInfo().tintIndex();
//      int color = -1;
//      if (tintIndex != -1) {
//        BlockTintSource tintSource = blockColors.getTintSource(state, tintIndex);
//        if (tintSource != null) {
//          color = tintSource.colorInWorld(state, level, pos);
//        }
//      }
//      instance.setColor(color);
//      VertexConsumer vertexConsumer = ibuffer.getBuffer(renderTypeForLayer(quad.materialInfo().layer()));
//      vertexConsumer.putBakedQuad(pose, quad, instance);
//    }
//  }

  private static RenderType renderTypeForLayer(ChunkSectionLayer layer) {
    return switch (layer) {
      case SOLID -> RenderTypes.solidMovingBlock();
      case CUTOUT -> RenderTypes.cutoutMovingBlock();
      case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
    };
  }
}
