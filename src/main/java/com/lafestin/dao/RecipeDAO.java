package com.lafestin.dao;

import com.lafestin.model.Recipe;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

// minimal recipe dao
public class RecipeDAO {
    public List<Recipe> getAllRecipes(int userId) throws SQLException {
        return new ArrayList<>(); // TODO: implement
    }
    public Recipe getRecipeById(int recipeId) throws SQLException {
        return null; // TODO: implement
    }
    public void deleteRecipe(int recipeId) throws SQLException {
        // TODO: implement
    }
}