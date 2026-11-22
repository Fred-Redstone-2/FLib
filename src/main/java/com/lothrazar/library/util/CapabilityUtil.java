package com.lothrazar.library.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class CapabilityUtil {

  public static boolean isItem( Direction facing, Level level, BlockPos facingPos) {
    if (facing == null) {
      return false;
    }
    return  CapabilityUtil.item(level, facingPos, facing.getOpposite()) != null ;
  }

  public static boolean isFluid(Direction facing, Level level, BlockPos facingPos){
    return isItem(facing, level, facingPos);
  }

  public static boolean isEnergy(Direction facing, Level level, BlockPos facingPos) {
    if (facing == null) {
      return false;
    }
    return  CapabilityUtil.energy(level, facingPos, facing.getOpposite()) != null ;
  }

  public static EnergyHandler energy(ItemStack stack) {
    return stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(stack));
  }

  public static EnergyHandler energy(Level level, BlockPos pos) {
    return energy(level,pos,null);
  }

  public static int energyStored(Level level, BlockPos pos) {
    var handler = energy(level,pos,null);
    return handler == null ? 0 : handler.getAmountAsInt();
  }
  public static EnergyHandler energy(Level level, BlockPos pos, Direction dir) {
    return level.getCapability(Capabilities.Energy.BLOCK, pos, dir);
  }

  public static ResourceHandler<FluidResource> fluid(Level level, BlockPos pos ) {
    return fluid(level,pos, (Direction)null);
  }

  public static ResourceHandler<FluidResource> fluid(Level level, BlockPos pos, BlockHitResult dir) {
    return fluid(level,pos,dir.getDirection());
  }

  public static ResourceHandler<FluidResource> fluid(ItemStack stack) {
    return stack.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(stack));
  }

  public static ResourceHandler<FluidResource> fluid(Level level, BlockPos pos, Direction dir) {
    return level.getCapability(Capabilities.Fluid.BLOCK, pos, dir);
  }

  public static ResourceHandler<ItemResource> item(Level level, BlockPos pos) {
    return  item(level, pos, null);
  }

  public static ResourceHandler<ItemResource> item(Level level, BlockPos pos, Direction dir) {
    return level.getCapability(Capabilities.Item.BLOCK, pos, dir);
  }

  public static ResourceHandler<ItemResource> item(ItemStack stack) {
      return stack.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(stack));
  }
}
