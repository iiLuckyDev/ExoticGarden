package io.github.thebusybiscuit.exoticgarden.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;

import io.github.thebusybiscuit.exoticgarden.ExoticGarden;
import io.github.thebusybiscuit.exoticgarden.ExoticGardenRecipeTypes;
import io.github.thebusybiscuit.exoticgarden.items.BonemealableItem;
import io.github.thebusybiscuit.exoticgarden.items.Crook;
import io.github.thebusybiscuit.exoticgarden.items.CustomFood;
import io.github.thebusybiscuit.exoticgarden.items.Kitchen;
import io.github.thebusybiscuit.exoticgarden.items.MagicalEssence;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.food.Juice;

public final class ResearchSetup {

    private static final int RESEARCH_ID_BASE = 86_000_000;
    private static final int RESEARCH_ID_RANGE = 1_000_000;
    private static final int DEFAULT_RESEARCH_COST = 14;

    private ResearchSetup() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void setup(@Nonnull ExoticGarden plugin) {
        List<SlimefunItem> addonItems = new ArrayList<>(
            Slimefun.getRegistry().getAllSlimefunItems().stream()
                .filter(item -> plugin.equals(item.getAddon()))
                .toList()
        );
        addonItems.sort(Comparator.comparing(SlimefunItem::getId));

        Set<Integer> usedResearchIds = new HashSet<>();
        List<String> defaultCostItems = new ArrayList<>();
        int registeredResearches = 0;

        for (SlimefunItem item : addonItems) {
            if (item.getResearch() != null) {
                continue;
            }

            int cost = getResearchCost(item);
            if (cost == DEFAULT_RESEARCH_COST) {
                defaultCostItems.add(item.getId());
            }

            Research research = new Research(
                new NamespacedKey(plugin, getResearchKey(item)),
                getResearchId(item, usedResearchIds),
                getResearchName(item),
                cost
            );
            research.addItems(item);
            research.register();
            registeredResearches++;
        }

        plugin.getLogger().info("Registered " + registeredResearches + " ExoticGarden researches.");

        if (!defaultCostItems.isEmpty()) {
            plugin.getLogger().warning("Used default research cost for: " + String.join(", ", defaultCostItems));
        }
    }

    private static int getResearchCost(@Nonnull SlimefunItem item) {
        String id = item.getId();

        if (item instanceof Kitchen) {
            return 30;
        }

        if (item instanceof MagicalEssence || id.endsWith("_PLANT") || id.endsWith("_ESSENCE")) {
            return 28;
        }

        if (item instanceof Crook) {
            return 12;
        }

        if (item instanceof BonemealableItem || ExoticGardenRecipeTypes.BREAKING_GRASS.equals(item.getRecipeType())) {
            return 8;
        }

        if (item instanceof Juice) {
            return id.endsWith("_SMOOTHIE") ? 14 : 12;
        }

        if (item instanceof CustomFood) {
            return 18;
        }

        RecipeType recipeType = item.getRecipeType();
        if (ExoticGardenRecipeTypes.HARVEST_TREE.equals(recipeType) || ExoticGardenRecipeTypes.HARVEST_BUSH.equals(recipeType)) {
            return 10;
        }

        if (RecipeType.GRIND_STONE.equals(recipeType)) {
            return 10;
        }

        if (RecipeType.JUICER.equals(recipeType)) {
            return 12;
        }

        if (RecipeType.ENHANCED_CRAFTING_TABLE.equals(recipeType)) {
            return 14;
        }

        return DEFAULT_RESEARCH_COST;
    }

    @Nonnull
    private static String getResearchKey(@Nonnull SlimefunItem item) {
        return "research_" + item.getId().toLowerCase(Locale.ROOT);
    }

    private static int getResearchId(@Nonnull SlimefunItem item, @Nonnull Set<Integer> usedResearchIds) {
        int candidate = RESEARCH_ID_BASE + Math.floorMod(("exoticgarden:" + item.getId()).hashCode(), RESEARCH_ID_RANGE);

        while (!usedResearchIds.add(candidate)) {
            candidate++;
        }

        return candidate;
    }

    @Nonnull
    private static String getResearchName(@Nonnull SlimefunItem item) {
        String strippedName = ChatColor.stripColor(item.getItemName());

        if (strippedName != null) {
            strippedName = strippedName.trim();
        }

        if (strippedName == null || strippedName.isBlank() || "Error".equalsIgnoreCase(strippedName)) {
            return item.getId().toLowerCase(Locale.ROOT).replace('_', ' ');
        }

        return strippedName;
    }
}
