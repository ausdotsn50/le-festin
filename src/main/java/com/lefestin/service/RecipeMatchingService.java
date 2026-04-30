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
 * RecipeMatchingService — ranks recipes by how much of each recipe's
 * ingredient quantity the user already has in their pantry.
 *
 * Algorithm:
 *  1. Load the user's pantry into a Map<ingredientId, PantryItem>
 *     for O(1) lookup per ingredient check
 *  2. For each recipe, fetch its required ingredients
 *  3. For each required ingredient, compare available quantity to
 *     required quantity
 *  4. matchPercent = (fulfilledQuantity / requiredQuantity) * 100,
 *     rounded to nearest int
 *  5. Collect ingredients that are still short into a list
 *  6. Sort all results by matchPercent descending, then by recipe title
 *
 * Note on unit matching:
 *  Unit comparison is intentionally skipped — checking whether
 *  "3 tablespoon soy sauce" satisfies "4 tablespoon soy sauce"
 *  requires unit conversion (tablespoon → teaspoon → ml) which is
 *  out of scope for this version. The match is quantity-aware only
 *  when the ingredient row uses the same stored unit semantics.
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
     *  - Compare available quantity against the recipe quantity
     *  - For missing ingredients, calculate the exact shortage
     *    (required - available, capped at 0)
     *  - Store the shortage amount in the missing ingredient's quantity field
     *
     * matchPercent = round((fulfilledQuantity / requiredQuantity) * 100)
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
        double fulfilledQuantity = 0.0;
        double requiredQuantity = 0.0;

        for (RecipeIngredient ri : required) {
            double recipeQty = Math.max(ri.getQuantity(), 0.0);
            requiredQuantity += recipeQty;

            PantryItem pantryItem = pantryMap.get(ri.getIngredientId());
            double availableQty = pantryItem != null ? pantryItem.getQuantity() : 0.0;
            double matchedQty = Math.min(availableQty, recipeQty);
            fulfilledQuantity += matchedQty;

            if (matchedQty < recipeQty) {
                // Calculate exact shortage: how much is still needed
                double shortage = recipeQty - availableQty;
                shortage = Math.max(shortage, 0.0); // cap at 0
                shortage = roundQty(shortage); // round to avoid float artifacts
                
                // Create a new RecipeIngredient with shortage quantity
                RecipeIngredient shortageIngredient = new RecipeIngredient(
                    ri.getRecipeId(),
                    ri.getIngredientId(),
                    shortage,
                    ri.getUnit(),
                    ri.getIngredientName()
                );
                missing.add(shortageIngredient);
            }
        }

        int matchPercent = requiredQuantity <= 0.0
            ? 100
            : (int) Math.round(fulfilledQuantity / requiredQuantity * 100);

        return new RecipeMatchResult(recipe, matchPercent, missing);
    }

    /**
     * Rounds quantity to 2 decimal places.
     * Prevents float artifacts like 0.30000000000000004
     * from appearing in the UI.
     */
    private double roundQty(double qty) {
        return Math.round(qty * 100.0) / 100.0;
    }
}