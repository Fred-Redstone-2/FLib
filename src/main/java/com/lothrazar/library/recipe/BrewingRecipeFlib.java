package com.lothrazar.library.recipe;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PotionIngredient;

public class BrewingRecipeFlib extends BrewingRecipe {

  private final ItemStack inputStack;

  public BrewingRecipeFlib(ItemStack inputStack, Ingredient ingredient, ItemStack output) {
    super(new PotionIngredient(Ingredient.of(inputStack.getItem()), Optional.empty()),
        new PotionIngredient(ingredient, Optional.empty()), ItemStackTemplate.fromStack(output));
    this.inputStack = inputStack.copy();
  }

  @Override
  public boolean matches(BrewingInput input) {
    PotionContents potion = input.input().get(DataComponents.POTION_CONTENTS);
    PotionContents expected = inputStack.get(DataComponents.POTION_CONTENTS);
    return super.matches(input) && potion != null && potion.equals(expected);
  }
}
