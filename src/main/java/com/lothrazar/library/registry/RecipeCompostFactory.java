package com.lothrazar.library.registry;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Configures compostable item components.
 *
 */
public class RecipeCompostFactory {

  public static final class FillValues {

    /**
     * leaves kelp grass seeds berries
     */
    public static final float LEAVES = 0.3F;
    /**
     * vines, cactus, sugar cane, melon slices
     */
    public static final float VINES = 0.5F;
    /**
     * flowers, mushrooms, moss
     */
    public static final float FLOWER = 0.65F;
    /**
     * Baked potato, bread, mushroom blocks, wart blocks
     */
    public static final float HAY = 0.85F;
    /**
     * cake, pumkin pie
     */
    public static final float FULL = 1F;
  }

  private static final Map<Item, ResolvableInt> COMPOSTABLES = new HashMap<>();

  public static void put(ItemLike itemLike, float val) {
    COMPOSTABLES.put(itemLike.asItem(), providerFor(val));
  }

  public static Item.Properties withChance(Item.Properties properties, float val) {
    return properties.component(DataComponents.COMPOSTABLE, new Compostable(providerFor(val)));
  }

  public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
    COMPOSTABLES.forEach((item, layers) -> event.modify(item,
        (components, registries, modifiedItem) -> components.set(DataComponents.COMPOSTABLE,
            new Compostable(layers))));
  }

  private static ResolvableInt providerFor(float val) {
    // The 26.3 component references registered providers, so approximate custom odds with the nearest vanilla tier.
    if (Float.isNaN(val) || val <= 0.15F) {
      return new ResolvableInt.Constant(0);
    }
    if (val <= 0.4F) {
      return ResolvableInt.fromKey(ContextIntProviders.COMPOSTABLE_LOW);
    }
    if (val <= 0.575F) {
      return ResolvableInt.fromKey(ContextIntProviders.COMPOSTABLE_LOW_MEDIUM);
    }
    if (val <= 0.75F) {
      return ResolvableInt.fromKey(ContextIntProviders.COMPOSTABLE_MEDIUM);
    }
    if (val <= 0.925F) {
      return ResolvableInt.fromKey(ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH);
    }
    return ResolvableInt.fromKey(ContextIntProviders.COMPOSTABLE_ALWAYS_ADD_ONE);
  }
}
