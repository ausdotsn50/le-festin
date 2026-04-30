package com.lefestin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.lefestin.config.DBConnection;
import com.lefestin.model.Recipe;

/**
 * RecipeDAO — all SQL for the `recipe` table.
 *
 * Rules followed throughout:
 *  - PreparedStatement only — never raw string concatenation
 *  - Connection is never closed here — owned by DBConnection singleton
 *  - PreparedStatement always closed via try-with-resources
 *  - SQLException is thrown up — never swallowed silently
 *  - After INSERT, generated key is retrieved and set back on the model
 */
public class RecipeDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Inserts a new recipe row.
     * Sets recipeId on the Recipe object from the generated key.
     *
     * @param r  Recipe with recipeId = 0 (not yet assigned)
     * @throws SQLException if the INSERT fails
     */
    public void addRecipe(Recipe r) throws SQLException {
        String sql = """
            INSERT INTO recipe (user_id, title, category, prep_time, `procedure`)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(   1, r.getUserId());
            stmt.setString(2, r.getTitle());
            stmt.setString(3, r.getCategory());
            stmt.setInt(   4, r.getPrepTime());
            stmt.setString(5, r.getProcedure());

            stmt.executeUpdate();

            // Retrieve the AUTO_INCREMENT id MySQL assigned
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setRecipeId(keys.getInt(1));
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns all recipes belonging to a user, ordered by title.
     *
     * @param userId  the logged-in user's ID
     * @return list of Recipe objects, empty list if none found
     * @throws SQLException if the SELECT fails
     */
    public List<Recipe> getAllRecipes(int userId) throws SQLException {
        String sql = """
            SELECT recipe_id, user_id, title, category, prep_time, `procedure`
            FROM recipe
            WHERE user_id = ?
            ORDER BY title ASC
            """;

        List<Recipe> recipes = new ArrayList<>();

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recipes.add(mapRow(rs));
                }
            }
        }

        return recipes;
    }

    /**
     * Returns a single recipe by its primary key.
     *
     * @param recipeId  the recipe's ID
     * @return the Recipe, or null if no row found
     * @throws SQLException if the SELECT fails
     */
    public Recipe getRecipeById(int recipeId) throws SQLException {
        String sql = """
            SELECT recipe_id, user_id, title, category, prep_time, `procedure`
            FROM recipe
            WHERE recipe_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, recipeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null; // not found
    }

    /**
     * Dynamic search — both keyword and category are optional.
     * Builds the WHERE clause only for the filters that are provided.
     *
     * @param userId    the logged-in user's ID
     * @param keyword   partial title match, or null/blank to skip
     * @param category  exact category match, or null/"All" to skip
     * @return filtered list ordered by title
     * @throws SQLException if the SELECT fails
     */
    public List<Recipe> searchRecipes(int userId,
                                      String keyword,
                                      String category) throws SQLException {
        boolean hasKeyword  = keyword  != null && !keyword.isBlank();
        boolean hasCategory = category != null
                              && !category.isBlank()
                              && !"All".equals(category);

        // Build SQL dynamically — only add clauses we need
        StringBuilder sql = new StringBuilder("""
            SELECT recipe_id, user_id, title, category, prep_time, `procedure`
            FROM recipe
            WHERE user_id = ?
            """);

        if (hasKeyword)  sql.append("  AND title    LIKE ?\n");
        if (hasCategory) sql.append("  AND category = ?\n");
        sql.append("ORDER BY title ASC");

        List<Recipe> recipes = new ArrayList<>();

        try (PreparedStatement stmt =
                conn().prepareStatement(sql.toString())) {

            int idx = 1;
            stmt.setInt(idx++, userId);
            if (hasKeyword)  stmt.setString(idx++, "%" + keyword + "%");
            if (hasCategory) stmt.setString(idx,   category);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recipes.add(mapRow(rs));
                }
            }
        }

        return recipes;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Updates all editable fields of an existing recipe.
     * Identified by recipeId — never updates user_id.
     *
     * @param r  Recipe with recipeId set to the row being updated
     * @throws SQLException if the UPDATE fails
     */
    public void updateRecipe(Recipe r) throws SQLException {
        String sql = """
            UPDATE recipe
            SET title     = ?,
                category  = ?,
                prep_time = ?,
                `procedure` = ?
            WHERE recipe_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setString(1, r.getTitle());
            stmt.setString(2, r.getCategory());
            stmt.setInt(   3, r.getPrepTime());
            stmt.setString(4, r.getProcedure());
            stmt.setInt(   5, r.getRecipeId());

            stmt.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Deletes a recipe by primary key.
     * CASCADE in the schema automatically removes all associated
     * recipe_ingredient and meal_entry rows.
     *
     * @param recipeId  the recipe to delete
     * @throws SQLException if the DELETE fails
     */
    public void deleteRecipe(int recipeId) throws SQLException {
        String sql = "DELETE FROM recipe WHERE recipe_id = ?";

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, recipeId);
            stmt.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Maps a single ResultSet row to a Recipe object.
     * Column names must match the `recipe` table exactly.
     * Called by every SELECT method — single place to fix if schema changes.
     */
    private Recipe mapRow(ResultSet rs) throws SQLException {
        return new Recipe(
            rs.getInt(   "recipe_id"),
            rs.getInt(   "user_id"),
            rs.getString("title"),
            rs.getString("category"),
            rs.getInt(   "prep_time"),
            rs.getString("procedure")
        );
    }
}