package com.lefestin.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import com.lefestin.dao.MealEntryDAO;
import com.lefestin.dao.PantryDAO;
import com.lefestin.dao.RecipeIngredientDAO;
import com.lefestin.model.MealEntry;
import com.lefestin.model.PantryItem;
import com.lefestin.model.RecipeIngredient;

/**
 * GroceryListService — calculates what a user still needs to buy
 * to execute their meal plan for a given date range.
 *
 * Algorithm:
 *  1. Fetch all meal entries in the date range
 *  2. For each entry, fetch that recipe's required ingredients
 *  3. Aggregate total required quantities across all recipes
 *     (same ingredient used in multiple recipes adds up)
 *  4. Load the user's pantry into a lookup map
 *  5. Subtract pantry stock from required quantities
 *  6. Return only ingredients where required > available
 *
 * Note on unit handling:
 *  Subtraction only applies when units match exactly.
 *  If a recipe needs "3 tablespoon soy sauce" and the pantry
 *  has "5 tablespoon soy sauce", 2 tablespoon is sufficient.
 *  If units differ ("3 tablespoon" vs "45 milliliter"), the
 *  pantry quantity is treated as zero for that ingredient —
 *  a UnitConverter class could handle this in a future version.
 */
public class GroceryListService {

    private final MealEntryDAO        mealEntryDAO;
    private final RecipeIngredientDAO riDAO;
    private final PantryDAO           pantryDAO;

    // ── Constructor ───────────────────────────────────────────────────────
    public GroceryListService() {
        this.mealEntryDAO = new MealEntryDAO();
        this.riDAO        = new RecipeIngredientDAO();
        this.pantryDAO    = new PantryDAO();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIMARY METHOD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns the grocery list for a date range — ingredients the user
     * needs to buy to cover all planned meals that they don't already have.
     *
     * @param userId  the logged-in user
     * @param from    first day of the range (inclusive)
     * @param to      last day of the range  (inclusive)
     * @return consolidated list of ingredients still needed,
     *         sorted by ingredient name; empty if pantry covers everything
     * @throws SQLException if any DAO call fails
     */
    public List<RecipeIngredient> getGroceryList(int userId,
                                                  LocalDate from,
                                                  LocalDate to)
            throws SQLException {

        // ── Step 1: fetch all meal entries in the range ────────────────────
        List<MealEntry> entries =
            mealEntryDAO.getEntriesByWeek(userId, from, to);

        if (entries.isEmpty()) {
            return new ArrayList<>(); // no meals planned — nothing to buy
        }

        // ── Step 2 + 3: aggregate total required quantities ────────────────
        // key   = "ingredientId|unit"  (unit-aware aggregation)
        // value = AggregatedIngredient (running total + display fields)
        Map<String, AggregatedIngredient> required =
            aggregateRequired(entries);

        if (required.isEmpty()) {
            return new ArrayList<>();
        }

        // ── Step 4: load pantry into lookup map ────────────────────────────
        // key = ingredientId, value = PantryItem
        Map<Integer, PantryItem> pantryMap = buildPantryMap(userId);

        // ── Step 5 + 6: subtract pantry, collect what's still needed ───────
        return subtractPantry(required, pantryMap);
    }

    /**
     * Convenience overload — grocery list for a single day.
     *
     * @param userId  the logged-in user
     * @param date    the day to check
     * @return ingredients needed for that day's meals
     * @throws SQLException if any DAO call fails
     */
    public List<RecipeIngredient> getGroceryListForDay(int userId,
                                                        LocalDate date)
            throws SQLException {
        return getGroceryList(userId, date, date);
    }

    /**
     * Returns a summary string for display in GroceryListPanel header.
     * e.g. "7 items needed for Apr 14 – Apr 20"
     *
     * @param userId  the logged-in user
     * @param from    start of range
     * @param to      end of range
     * @return human-readable summary
     * @throws SQLException if any DAO call fails
     */
    public String getSummary(int userId,
                              LocalDate from,
                              LocalDate to)
            throws SQLException {
        List<RecipeIngredient> list = getGroceryList(userId, from, to);

        if (list.isEmpty()) {
            return "Pantry covers all planned meals";
        }

        java.time.format.DateTimeFormatter fmt =
            java.time.format.DateTimeFormatter.ofPattern("MMM d");

        return list.size() + " item"
            + (list.size() == 1 ? "" : "s")
            + " needed for "
            + from.format(fmt)
            + (from.equals(to) ? "" : " – " + to.format(fmt));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Internal holder for aggregating ingredient quantities
     * across multiple recipes before pantry subtraction.
     */
    private static class AggregatedIngredient {
        final int    ingredientId;
        final String ingredientName;
        final String unit;
        double       totalRequired;

        AggregatedIngredient(int ingredientId,
                              String ingredientName,
                              String unit,
                              double initialQty) {
            this.ingredientId   = ingredientId;
            this.ingredientName = ingredientName;
            this.unit           = unit;
            this.totalRequired  = initialQty;
        }
    }

    /**
     * Fetches ingredients for every meal entry and aggregates
     * quantities by (ingredientId + unit) key.
     *
     * Why include unit in the key:
     * If recipe A needs "2 cup rice" and recipe B needs "1 cup rice",
     * the total is "3 cup rice" — same unit, addable.
     * If recipe A needs "200 gram butter" and recipe B needs
     * "2 tablespoon butter", they cannot be added without unit
     * conversion, so they are kept as separate line items.
     */
    private Map<String, AggregatedIngredient> aggregateRequired(
            List<MealEntry> entries) throws SQLException {

        Map<String, AggregatedIngredient> required = new LinkedHashMap<>();

        // Track which recipeIds we've already fetched to avoid
        // re-fetching ingredients when the same recipe appears
        // in multiple meal slots across the week
        Set<Integer> processedRecipes = new HashSet<>();

        for (MealEntry entry : entries) {
            int recipeId = entry.getRecipeId();

            if (processedRecipes.contains(recipeId)) {
                continue; // same recipe in multiple slots — count once
            }
            processedRecipes.add(recipeId);

            List<RecipeIngredient> ingredients =
                riDAO.getIngredientsByRecipeId(recipeId);

            for (RecipeIngredient ri : ingredients) {
                // Key combines ingredientId + unit for unit-aware grouping
                String key = ri.getIngredientId() + "|" + ri.getUnit();

                if (required.containsKey(key)) {
                    // Same ingredient + same unit — add quantities
                    required.get(key).totalRequired += ri.getQuantity();
                } else {
                    // New ingredient or new unit variant — add entry
                    required.put(key, new AggregatedIngredient(
                        ri.getIngredientId(),
                        ri.getIngredientName(),
                        ri.getUnit(),
                        ri.getQuantity()
                    ));
                }
            }
        }

        return required;
    }

    /**
     * Builds a Map<ingredientId, PantryItem> for O(1) pantry lookup.
     */
    private Map<Integer, PantryItem> buildPantryMap(int userId)
            throws SQLException {
        List<PantryItem> items = pantryDAO.getPantryByUser(userId);
        Map<Integer, PantryItem> map = new HashMap<>();
        for (PantryItem item : items) {
            map.put(item.getIngredientId(), item);
        }
        return map;
    }

    /**
     * Subtracts pantry stock from required quantities.
     * Returns only ingredients where required > available.
     *
     * Subtraction rules:
     *  - Pantry has the ingredient, same unit → subtract quantity
     *  - Pantry has the ingredient, different unit → treat as 0 available
     *  - Pantry doesn't have the ingredient → full amount needed
     *  - Required quantity after subtraction ≤ 0 → skip (covered)
     */
    private List<RecipeIngredient> subtractPantry(
            Map<String, AggregatedIngredient> required,
            Map<Integer, PantryItem> pantryMap) {

        List<RecipeIngredient> groceryList = new ArrayList<>();

        for (AggregatedIngredient agg : required.values()) {
            double stillNeeded = agg.totalRequired;

            PantryItem pantryItem = pantryMap.get(agg.ingredientId);

            if (pantryItem != null) {
                if (pantryItem.getUnit().equalsIgnoreCase(agg.unit)) {
                    // Same unit — subtract what's available
                    stillNeeded -= pantryItem.getQuantity();
                }
                // Different unit — can't subtract safely, full amount needed
                // stillNeeded remains at totalRequired
            }

            // Only add to grocery list if something is still needed
            if (stillNeeded > 0) {
                groceryList.add(new RecipeIngredient(
                    0,                   // no recipeId — this is a shopping item
                    agg.ingredientId,
                    roundQty(stillNeeded),
                    agg.unit,
                    agg.ingredientName
                ));
            }
        }

        // Sort alphabetically by ingredient name
        groceryList.sort((a, b) ->
            a.getIngredientName()
             .compareToIgnoreCase(b.getIngredientName()));

        return groceryList;
    }

    /**
     * Rounds quantity to 2 decimal places.
     * Prevents 0.30000000000000004 style float artifacts
     * from appearing in the grocery list UI.
     */
    private double roundQty(double qty) {
        return Math.round(qty * 100.0) / 100.0;
    }
}