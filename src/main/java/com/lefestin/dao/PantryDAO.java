package com.lefestin.dao;
import java.util.List;
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
public interface PantryDAO {
    void addPantryItem(PantryItem p);
    void addOrUpdate(PantryItem p);
    List<PantryItem> getPantryByUser(int userId);
    PantryItem getPantryItem(int ingredientId, int userId);
    boolean existsInPantry(int ingredientId, int userId);
    void updateQuantity(int ingredientId,
                               int userId,
                               double qty); 

    void updateQuantityAndUnit(int ingredientId,
                                      int userId,
                                      double qty,
                                      String unit);
    void deletePantryItem(int ingredientId, int userId);
    void clearPantry(int userId);
}