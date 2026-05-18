package dao;
import java.util.List;
import model.Ingredient;

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
public interface IngredientDAO {
    void addIngredient(Ingredient i);
    Ingredient findOrCreate(String name);
    List<Ingredient> getAllIngredients();
    Ingredient getIngredientById(int id);
    List<Ingredient> searchByName(String name);
    boolean existsByName(String name);
    void deleteIngredient(int id);
}