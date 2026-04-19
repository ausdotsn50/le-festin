package com.lefestin.model;

import java.util.List;

/**
 * RecipeMatchResult — returned by RecipeMatchingService.
 *
 * Not a DB table — purely a calculation result used by
 * RecipeSuggestionsPanel to render each recipe card with
 * its match percentage and missing ingredient list.
 */
public class RecipeMatchResult {

    private final Recipe                 recipe;
    private final int                    matchPercent;
    private final List<RecipeIngredient> missingIngredients;

    // ── Constructor ───────────────────────────────────────────────────────
    public RecipeMatchResult(Recipe recipe,
                             int matchPercent,
                             List<RecipeIngredient> missingIngredients) {
        this.recipe             = recipe;
        this.matchPercent       = matchPercent;
        this.missingIngredients = missingIngredients;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public Recipe                 getRecipe()             { return recipe;             }
    public int                    getMatchPercent()       { return matchPercent;       }
    public List<RecipeIngredient> getMissingIngredients() { return missingIngredients; }

    // ── Convenience ───────────────────────────────────────────────────────

    /** True if the user has every ingredient — ready to cook now. */
    public boolean isFullMatch() {
        return matchPercent == 100;
    }

    /** True if the user has at least one ingredient. */
    public boolean isPartialMatch() {
        return matchPercent > 0 && matchPercent < 100;
    }

    /** How many ingredients are still needed. */
    public int getMissingCount() {
        return missingIngredients.size();
    }

    /**
     * Formatted label for the match percentage badge in the UI.
     * 100 → "Ready to cook"
     *  75 → "75% match"
     *   0 → "0% match"
     */
    public String getMatchLabel() {
        return matchPercent == 100
            ? "Ready to cook"
            : matchPercent + "% match";
    }

    // ── toString ──────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "RecipeMatchResult{" +
            "recipe='"      + recipe.getTitle() + '\'' +
            ", match="      + matchPercent       + "%" +
            ", missing="    + getMissingCount()  +
            '}';
    }
}