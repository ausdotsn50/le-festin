package com.lefestin.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.lefestin.config.DBConnection;
import com.lefestin.model.PantryItem;

/**
 * PantryDAO — all SQL for the `pantry` table.
 *
 * pantry(ingredient_id FK, user_id FK, quantity, unit)
 * Composite PK: (ingredient_id, user_id)
 *
 * Scoped by user_id — every query filters by the logged-in user.
 * JOINs with ingredient table so ingredientName is always populated.
 */
public class PantryDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Inserts a new pantry item for a user.
     *
     * Throws if (ingredient_id, user_id) already exists —
     * composite PK constraint. Callers should check with
     * existsInPantry() first, or use addOrUpdate() instead.
     *
     * @param p  PantryItem with ingredientId and userId set
     * @throws SQLException if INSERT fails or duplicate PK
     */
    public void addPantryItem(PantryItem p) throws SQLException {
        String sql = """
            INSERT INTO pantry (ingredient_id, user_id, quantity, unit)
            VALUES (?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(   1, p.getIngredientId());
            stmt.setInt(   2, p.getUserId());
            stmt.setDouble(3, p.getQuantity());
            stmt.setString(4, p.getUnit());

            stmt.executeUpdate();
        }
    }

    /**
     * Inserts a new pantry item or updates quantity + unit if the
     * ingredient already exists in this user's pantry.
     *
     * Uses MySQL's INSERT ... ON DUPLICATE KEY UPDATE — atomic,
     * no race condition between check and insert.
     *
     * Use this in AddEditIngredientDialog instead of addPantryItem()
     * to avoid duplicate key exceptions.
     *
     * @param p  PantryItem to add or update
     * @throws SQLException if operation fails
     */
    public void addOrUpdate(PantryItem p) throws SQLException {
        String sql = """
            INSERT INTO pantry (ingredient_id, user_id, quantity, unit)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                quantity = VALUES(quantity),
                unit     = VALUES(unit)
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(   1, p.getIngredientId());
            stmt.setInt(   2, p.getUserId());
            stmt.setDouble(3, p.getQuantity());
            stmt.setString(4, p.getUnit());

            stmt.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns all pantry items for a user, JOINed with ingredient
     * table so ingredientName is populated on each object.
     * Ordered by ingredient name A→Z.
     *
     * @param userId  the logged-in user's ID
     * @return list of PantryItem with ingredientName set,
     *         empty list if pantry is empty
     * @throws SQLException if SELECT fails
     */
    public List<PantryItem> getPantryByUser(int userId)
            throws SQLException {
        String sql = """
            SELECT
                p.ingredient_id,
                p.user_id,
                p.quantity,
                p.unit,
                i.name AS ingredient_name
            FROM pantry p
            JOIN ingredient i
                ON p.ingredient_id = i.ingredient_id
            WHERE p.user_id = ?
            ORDER BY i.name ASC
            """;

        List<PantryItem> list = new ArrayList<>();

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    /**
     * Returns a single pantry item for a specific ingredient + user.
     * Used by RecipeMatchingService to check exact quantity available.
     *
     * @param ingredientId  the ingredient to look up
     * @param userId        the user whose pantry to check
     * @return PantryItem with ingredientName set, or null if not found
     * @throws SQLException if SELECT fails
     */
    public PantryItem getPantryItem(int ingredientId, int userId)
            throws SQLException {
        String sql = """
            SELECT
                p.ingredient_id,
                p.user_id,
                p.quantity,
                p.unit,
                i.name AS ingredient_name
            FROM pantry p
            JOIN ingredient i
                ON p.ingredient_id = i.ingredient_id
            WHERE p.ingredient_id = ?
              AND p.user_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, ingredientId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    /**
     * Checks whether an ingredient already exists in a user's pantry.
     * Use before addPantryItem() to avoid duplicate PK exceptions.
     *
     * @param ingredientId  ingredient to check
     * @param userId        user whose pantry to check
     * @return true if the pantry row exists
     * @throws SQLException if SELECT fails
     */
    public boolean existsInPantry(int ingredientId, int userId)
            throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM pantry
            WHERE ingredient_id = ?
              AND user_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, ingredientId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Updates the quantity of an existing pantry item.
     * Called from AddEditIngredientDialog edit mode.
     *
     * @param ingredientId  identifies the pantry row (composite PK part 1)
     * @param userId        identifies the pantry row (composite PK part 2)
     * @param qty           new quantity value
     * @throws SQLException if UPDATE fails
     */
    public void updateQuantity(int ingredientId,
                               int userId,
                               double qty) throws SQLException {
        String sql = """
            UPDATE pantry
            SET quantity = ?
            WHERE ingredient_id = ?
              AND user_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setDouble(1, qty);
            stmt.setInt(   2, ingredientId);
            stmt.setInt(   3, userId);

            stmt.executeUpdate();
        }
    }

    /**
     * Updates both quantity and unit of an existing pantry item.
     * Use when the user changes unit as well as quantity in the dialog.
     *
     * @param ingredientId  composite PK part 1
     * @param userId        composite PK part 2
     * @param qty           new quantity
     * @param unit          new unit
     * @throws SQLException if UPDATE fails
     */
    public void updateQuantityAndUnit(int ingredientId,
                                      int userId,
                                      double qty,
                                      String unit) throws SQLException {
        String sql = """
            UPDATE pantry
            SET quantity = ?,
                unit     = ?
            WHERE ingredient_id = ?
              AND user_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setDouble(1, qty);
            stmt.setString(2, unit);
            stmt.setInt(   3, ingredientId);
            stmt.setInt(   4, userId);

            stmt.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Removes one ingredient from a user's pantry.
     * Identified by composite PK (ingredient_id, user_id).
     *
     * @param ingredientId  the ingredient to remove
     * @param userId        the user whose pantry to modify
     * @throws SQLException if DELETE fails
     */
    public void deletePantryItem(int ingredientId, int userId)
            throws SQLException {
        String sql = """
            DELETE FROM pantry
            WHERE ingredient_id = ?
              AND user_id = ?
            """;

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, ingredientId);
            stmt.setInt(2, userId);

            stmt.executeUpdate();
        }
    }

    /**
     * Clears the entire pantry for a user.
     * Used for "Reset Pantry" feature if added later.
     *
     * @param userId  user whose entire pantry to wipe
     * @throws SQLException if DELETE fails
     */
    public void clearPantry(int userId) throws SQLException {
        String sql = "DELETE FROM pantry WHERE user_id = ?";

        try (PreparedStatement stmt = conn().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Maps one ResultSet row to a PantryItem.
     * Uses the 5-arg constructor that includes ingredientName from JOIN.
     */
    private PantryItem mapRow(ResultSet rs) throws SQLException {
        return new PantryItem(
            rs.getInt(   "ingredient_id"),
            rs.getInt(   "user_id"),
            rs.getDouble("quantity"),
            rs.getString("unit"),
            rs.getString("ingredient_name") // from JOIN
        );
    }
}