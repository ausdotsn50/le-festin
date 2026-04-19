package com.lefestin.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.lefestin.config.DBConnection;
import com.lefestin.model.Ingredient;

/**
 * IngredientDAO — all SQL for the `ingredient` table.
 *
 * ingredient(ingredient_id PK, name UNIQUE)
 *
 * This is a global table — not scoped by user_id.
 * Every user shares the same ingredient pool.
 * Used by:
 *   - RecipeIngredientDAO  (linking ingredients to recipes)
 *   - PantryDAO            (linking ingredients to a user's pantry)
 *   - AddEditRecipeDialog  (ingredient dropdown)
 *   - AddEditIngredientDialog (ingredient dropdown)
 */
public class IngredientDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Inserts a new ingredient into the global ingredient pool.
     * Sets ingredientId on the Ingredient object from the generated key.
     *
     * Throws if name already exists — ingredient.name has a UNIQUE
     * constraint. Callers should check with searchByName() first
     * or catch the duplicate key exception.
     *
     * @param i  Ingredient with ingredientId = 0
     * @throws SQLException if INSERT fails or name already exists
     */
    public void addIngredient(Ingredient i) throws SQLException {
        String sql = "INSERT INTO ingredient (name) VALUES (?)";

        try (PreparedStatement stmt = conn().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, i.getName().trim().toLowerCase());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    i.setIngredientId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Returns an existing ingredient by name, or inserts and returns
     * a new one if not found.
     *
     * Use this in AddEditRecipeDialog when the user types a new
     * ingredient name that doesn't exist yet — avoids a duplicate
     * key exception and removes the need for a pre-check.
     *
     * @param name  ingredient name to find or create
     * @return existing or newly created Ingredient with id set
     * @throws SQLException if DB operation fails
     */
    public Ingredient findOrCreate(String name) throws SQLException {
        String normalized = name.trim().toLowerCase();

        // Try to find existing first
        List<Ingredient> existing = searchByName(normalized);
        for (Ingredient i : existing) {
            if (i.getName().equalsIgnoreCase(normalized)) {
                return i; // exact match found
            }
        }

        // Not found — insert new
        Ingredient newIngredient = new Ingredient(normalized);
        addIngredient(newIngredient);
        return newIngredient;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns every ingredient ordered by name A→Z.
     * Used to populate dropdowns in AddEditRecipeDialog
     * and AddEditIngredientDialog.
     *
     * @return all ingredients ordered by name, empty list if none
     * @throws SQLException if SELECT fails
     */
    public List<Ingredient> getAllIngredients() throws SQLException {
        String sql = """
            SELECT ingredient_id, name
            FROM ingredient
            ORDER BY name ASC
            """;

        List<Ingredient> list = new ArrayList<>();

        try (PreparedStatement stmt = conn().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    /**
     * Returns a single ingredient by primary key.
     *
     * @param id  ingredient_id to look up
     * @return the Ingredient, or null if not found
     * @throws SQLException if SELECT fails
     */
    public Ingredient getIngredientById(int id) throws SQLException {
        String sql = """
            SELECT ingredient_id, name
            FROM ingredient
            WHERE ingredient_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    /**
     * Partial name search using LIKE — case-insensitive.
     * Used by AddEditIngredientDialog to check for existing
     * ingredients before inserting a new one.
     *
     * @param name  partial or full name to search for
     * @return list of matching ingredients ordered by name,
     *         empty list if none match
     * @throws SQLException if SELECT fails
     */
    public List<Ingredient> searchByName(String name)
            throws SQLException {
        String sql = """
            SELECT ingredient_id, name
            FROM ingredient
            WHERE name LIKE ?
            ORDER BY name ASC
            """;

        List<Ingredient> list = new ArrayList<>();

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setString(1, "%" + name.trim() + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    /**
     * Exact name lookup — used to check for duplicates before insert.
     *
     * @param name  exact ingredient name to check
     * @return true if an ingredient with this name already exists
     * @throws SQLException if SELECT fails
     */
    public boolean existsByName(String name) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM ingredient
            WHERE name = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setString(1, name.trim().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Deletes an ingredient by primary key.
     *
     * CASCADE in the schema automatically removes associated rows
     * from recipe_ingredient and pantry — so deleting "garlic" here
     * removes it from every recipe and every user's pantry.
     *
     * This is destructive — the UI should warn the user before calling.
     *
     * @param id  ingredient_id to delete
     * @throws SQLException if DELETE fails
     */
    public void deleteIngredient(int id) throws SQLException {
        String sql = "DELETE FROM ingredient WHERE ingredient_id = ?";

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Maps one ResultSet row to an Ingredient.
     */
    private Ingredient mapRow(ResultSet rs) throws SQLException {
        return new Ingredient(
            rs.getInt(   "ingredient_id"),
            rs.getString("name")
        );
    }
}