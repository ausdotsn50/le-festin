package com.lefestin.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.lefestin.config.DBConnection;
import com.lefestin.model.RecipeIngredient;

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
public interface RecipeIngredientDAO {
    void addRecipeIngredient(RecipeIngredient ri);
    void addAll(List<RecipeIngredient> ingredients);
    List<RecipeIngredient> getIngredientsByRecipeId(int recipeId);
    void deleteByRecipeId(int recipeId);
    void deleteByRecipeAndIngredient(int recipeId,
                                             int ingredientId);
}