package com.lafestin.dao;

import com.lafestin.config.DBConnection;
import com.lafestin.model.RecipeIngredient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * RecipeIngredientDAO — all SQL for the `recipe_ingredient` junction table.
 *
 * Composite PK: (recipe_id, ingredient_id) — no surrogate key.
 *
 * Key behavior:
 *  - addRecipeIngredient: INSERT one ingredient row for a recipe
 *  - getIngredientsByRecipeId: JOIN with ingredient table to include name
 *  - deleteByRecipeId: wipe all ingredient rows for a recipe
 *    (called before re-saving from AddEditRecipeDialog)
 */
public class RecipeIngredientDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Inserts one ingredient row for a recipe.
     * Call once per ingredient when saving from AddEditRecipeDialog.
     *
     * @param ri  RecipeIngredient with recipeId + ingredientId already set
     * @throws SQLException if INSERT fails or FK constraint violated
     */
    public void addRecipeIngredient(RecipeIngredient ri)
            throws SQLException {
        String sql = """
            INSERT INTO recipe_ingredient
                (recipe_id, ingredient_id, quantity, unit)
            VALUES (?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(   1, ri.getRecipeId());
            stmt.setInt(   2, ri.getIngredientId());
            stmt.setDouble(3, ri.getQuantity());
            stmt.setString(4, ri.getUnit());

            stmt.executeUpdate();
        }
    }

    /**
     * Inserts a full list of ingredient rows in one method call.
     * Convenience wrapper used by AddEditRecipeDialog after
     * deleteByRecipeId() clears old rows.
     *
     * @param ingredients  list of RecipeIngredient rows to insert
     * @throws SQLException if any INSERT fails
     */
    public void addAll(List<RecipeIngredient> ingredients)
            throws SQLException {
        for (RecipeIngredient ri : ingredients) {
            addRecipeIngredient(ri);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns all ingredient rows for a recipe, JOINed with the
     * ingredient table so ingredientName is populated on each object.
     *
     * The UI uses ingredientName directly — no second lookup needed.
     *
     * @param recipeId  the recipe whose ingredients to fetch
     * @return list of RecipeIngredient with ingredientName set,
     *         ordered by ingredient name; empty list if none found
     * @throws SQLException if SELECT fails
     */
    public List<RecipeIngredient> getIngredientsByRecipeId(int recipeId)
            throws SQLException {
        String sql = """
            SELECT
                ri.recipe_id,
                ri.ingredient_id,
                ri.quantity,
                ri.unit,
                i.name AS ingredient_name
            FROM recipe_ingredient ri
            JOIN ingredient i
                ON ri.ingredient_id = i.ingredient_id
            WHERE ri.recipe_id = ?
            ORDER BY i.name ASC
            """;

        List<RecipeIngredient> list = new ArrayList<>();

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, recipeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Deletes ALL ingredient rows for a given recipe.
     *
     * Used by AddEditRecipeDialog save flow:
     *   1. deleteByRecipeId(recipeId)   ← wipe old rows
     *   2. addAll(newIngredients)        ← insert fresh rows
     *
     * This is simpler and safer than diffing old vs new rows.
     *
     * @param recipeId  the recipe whose ingredient rows to wipe
     * @throws SQLException if DELETE fails
     */
    public void deleteByRecipeId(int recipeId) throws SQLException {
        String sql = """
            DELETE FROM recipe_ingredient
            WHERE recipe_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, recipeId);
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes a single specific ingredient row from a recipe.
     * Used when the user removes one row in the ingredient table
     * without saving the whole recipe again.
     *
     * @param recipeId      the recipe
     * @param ingredientId  the specific ingredient to remove
     * @throws SQLException if DELETE fails
     */
    public void deleteByRecipeAndIngredient(int recipeId,
                                             int ingredientId)
            throws SQLException {
        String sql = """
            DELETE FROM recipe_ingredient
            WHERE recipe_id = ?
              AND ingredient_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, recipeId);
            stmt.setInt(2, ingredientId);
            stmt.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Maps one ResultSet row to a RecipeIngredient.
     * Uses the 5-arg constructor that includes ingredientName.
     */
    private RecipeIngredient mapRow(ResultSet rs) throws SQLException {
        return new RecipeIngredient(
            rs.getInt(   "recipe_id"),
            rs.getInt(   "ingredient_id"),
            rs.getDouble("quantity"),
            rs.getString("unit"),
            rs.getString("ingredient_name")  // from JOIN
        );
    }
}