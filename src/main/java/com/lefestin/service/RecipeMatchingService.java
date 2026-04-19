package com.lefestin.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lefestin.dao.PantryDAO;
import com.lefestin.dao.RecipeDAO;
import com.lefestin.dao.RecipeIngredientDAO;
import com.lefestin.model.PantryItem;
import com.lefestin.model.Recipe;
import com.lefestin.model.RecipeIngredient;
import com.lefestin.model.RecipeMatchResult;

/**
 * RecipeMatchingService — ranks recipes by how many of their
 * ingredients the user already has in their pantry.
 *
 * Algorithm:
 *  1. Load the user's pantry into a Map<ingredientId, PantryItem>
 *     for O(1) lookup per ingredient check
 *  2. For each recipe, fetch its required ingredients
 *  3. For each required ingredient, check if it exists in the pantry map
 *  4. matchPercent = (present / total) * 100, rounded to nearest int
 *  5. Collect missing ingredients into a list
 *  6. Sort all results by matchPercent descending, then by recipe title
 *
 * Note on unit matching:
 *  Unit comparison is intentionally skipped — checking whether
 *  "3 tablespoon soy sauce" satisfies "4 tablespoon soy sauce"
 *  requires unit conversion (tablespoon → teaspoon → ml) which is
 *  out of scope for this version. The match is ingredient presence only.
 *  A future version could add a UnitConverter utility class.
 */
public class RecipeMatchingService {

    private final PantryDAO           pantryDAO;
    private final RecipeDAO           recipeDAO;
    private final RecipeIngredientDAO riDAO;

    // ── Constructor ───────────────────────────────────────────────────────
    public RecipeMatchingService() {
        this.pantryDAO = new PantryDAO();
        this.recipeDAO = new RecipeDAO();
        this.riDAO     = new RecipeIngredientDAO();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIMARY METHOD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns all recipes ranked by pantry match percentage.
     *
     * @param userId  the logged-in user
     * @return list of RecipeMatchResult sorted by matchPercent
     *         descending, then recipe title ascending;
     *         empty list if user has no recipes
     * @throws SQLException if any DAO call fails
     */
    public List<RecipeMatchResult> getMatchedRecipes(int userId)
            throws SQLException {

        // ── Step 1: load pantry into a fast lookup map ─────────────────────
        // key = ingredientId, value = PantryItem
        // O(1) lookup replaces O(n) list scan for every ingredient check
        Map<Integer, PantryItem> pantryMap = buildPantryMap(userId);

        // ── Step 2: load all recipes for this user ─────────────────────────
        List<Recipe> recipes = recipeDAO.getAllRecipes(userId);

        // ── Step 3: score each recipe ──────────────────────────────────────
        List<RecipeMatchResult> results = new ArrayList<>();

        for (Recipe recipe : recipes) {
            RecipeMatchResult result = scoreRecipe(recipe, pantryMap);
            results.add(result);
        }

        // ── Step 4: sort — 100% first, then descending, ties by title ──────
        results.sort((a, b) -> {
            int percentDiff = b.getMatchPercent() - a.getMatchPercent();
            if (percentDiff != 0) return percentDiff;
            return a.getRecipe().getTitle()
                    .compareToIgnoreCase(b.getRecipe().getTitle());
        });

        return results;
    }

    /**
     * Returns only recipes the user can make right now — 100% match.
     * Convenience method for a "Cook Now" filter in the UI.
     *
     * @param userId  the logged-in user
     * @return list of fully matched RecipeMatchResult, may be empty
     * @throws SQLException if any DAO call fails
     */
    public List<RecipeMatchResult> getReadyToCook(int userId)
            throws SQLException {
        List<RecipeMatchResult> all = getMatchedRecipes(userId);
        List<RecipeMatchResult> ready = new ArrayList<>();
        for (RecipeMatchResult r : all) {
            if (r.isFullMatch()) ready.add(r);
        }
        return ready;
    }

    /**
     * Returns the match result for one specific recipe.
     * Used by RecipeDetailPanel to show pantry status for a single recipe.
     *
     * @param userId    the logged-in user
     * @param recipeId  the recipe to score
     * @return RecipeMatchResult for that recipe, or null if recipe not found
     * @throws SQLException if any DAO call fails
     */
    public RecipeMatchResult getMatchForRecipe(int userId, int recipeId)
            throws SQLException {
        Recipe recipe = recipeDAO.getRecipeById(recipeId);
        if (recipe == null) return null;

        Map<Integer, PantryItem> pantryMap = buildPantryMap(userId);
        return scoreRecipe(recipe, pantryMap);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Loads the user's pantry into a Map<ingredientId, PantryItem>.
     * Called once per getMatchedRecipes() call — not per recipe.
     */
    private Map<Integer, PantryItem> buildPantryMap(int userId)
            throws SQLException {
        List<PantryItem> pantryItems = pantryDAO.getPantryByUser(userId);
        Map<Integer, PantryItem> pantryMap = new HashMap<>();

        for (PantryItem item : pantryItems) {
            pantryMap.put(item.getIngredientId(), item);
        }

        return pantryMap;
    }

    /**
     * Scores one recipe against the pantry map.
     *
     * For each required ingredient:
     *  - If ingredientId exists in pantryMap → present
     *  - Otherwise → missing, add to missing list
     *
     * matchPercent = round((present / total) * 100)
     * Edge case: recipe with 0 ingredients = 100% match
     * (nothing required = nothing missing)
     */
    private RecipeMatchResult scoreRecipe(
            Recipe recipe,
            Map<Integer, PantryItem> pantryMap) throws SQLException {

        List<RecipeIngredient> required =
            riDAO.getIngredientsByRecipeId(recipe.getRecipeId());

        // Edge case — recipe has no ingredients defined yet
        if (required.isEmpty()) {
            return new RecipeMatchResult(recipe, 100, new ArrayList<>());
        }

        List<RecipeIngredient> missing = new ArrayList<>();
        int present = 0;

        for (RecipeIngredient ri : required) {
            if (pantryMap.containsKey(ri.getIngredientId())) {
                present++;
            } else {
                missing.add(ri);
            }
        }

        // Round to nearest integer — 3/4 = 75%, 1/3 = 33%
        int matchPercent = (int) Math.round(
            (double) present / required.size() * 100
        );

        return new RecipeMatchResult(recipe, matchPercent, missing);
    }
}