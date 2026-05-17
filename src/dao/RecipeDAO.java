package com.lefestin.dao;
import java.util.List;
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
public interface RecipeDAO {
    void addRecipe(Recipe r);
    List<Recipe> getAllRecipes(int userId);
    Recipe getRecipeById(int recipeId);
    List<Recipe> searchRecipes(int userId,
                                      String keyword,
                                      String category);                                
    void updateRecipe(Recipe r);
    void deleteRecipe(int recipeId);
}