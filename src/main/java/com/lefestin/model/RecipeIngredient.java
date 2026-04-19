package com.lefestin.model;

/**
 * RecipeIngredient — maps to the `recipe_ingredient` junction table.
 *
 * recipe_ingredient(recipe_id FK, ingredient_id FK, quantity, unit)
 *
 * Represents one ingredient row inside a specific recipe.
 * Composite PK: (recipe_id, ingredient_id) — no surrogate key needed.
 */
public class RecipeIngredient {

    // Fields
    private int    recipeId;
    private int    ingredientId;
    private double quantity;
    private String unit;

    // Denormalized display field — NOT stored in DB 
    // Populated by JOIN queries in RecipeIngredientDAO so the UI
    // can show "paprika" instead of ingredient_id=12.
    private String ingredientName;

    // Constructor: full (used when reading from ResultSet)
    public RecipeIngredient(int recipeId, int ingredientId,
                            double quantity, String unit) {
        this.recipeId     = recipeId;
        this.ingredientId = ingredientId;
        this.quantity     = quantity;
        this.unit         = unit;
    }

    // Constructor: with name (used when reading from JOIN query)
    public RecipeIngredient(int recipeId, int ingredientId,
                            double quantity, String unit,
                            String ingredientName) {
        this(recipeId, ingredientId, quantity, unit);
        this.ingredientName = ingredientName;
    }

    // Default constructor 
    public RecipeIngredient() {}

    // Getters
    public int    getRecipeId()       { return recipeId;       }
    public int    getIngredientId()   { return ingredientId;   }
    public double getQuantity()       { return quantity;       }
    public String getUnit()           { return unit;           }
    public String getIngredientName() { return ingredientName; }

    // Setters
    public void setRecipeId(int recipeId)             { this.recipeId       = recipeId;       }
    public void setIngredientId(int ingredientId)     { this.ingredientId   = ingredientId;   }
    public void setQuantity(double quantity)           { this.quantity       = quantity;       }
    public void setUnit(String unit)                   { this.unit           = unit;           }
    public void setIngredientName(String name)         { this.ingredientName = name;           }

    // Convenience: formatted quantity + unit for UI display 
    // Prints "3 eggs", "0.5 cup", "500 gram" cleanly
    public String getFormattedAmount() {
        // Strip trailing .0 for whole numbers: 3.0 → "3", 0.5 → "0.5"
        String qty = (quantity == Math.floor(quantity))
            ? String.valueOf((int) quantity)
            : String.valueOf(quantity);
        String label = (ingredientName != null) ? ingredientName : "ingredient #" + ingredientId;
        return qty + " " + unit + " " + label;
    }

    @Override
    public String toString() {
        return "RecipeIngredient{" +
            "recipeId="      + recipeId     +
            ", ingredientId=" + ingredientId +
            ", quantity="    + quantity     +
            ", unit='"       + unit         + '\'' +
            (ingredientName != null ? ", name='" + ingredientName + '\'' : "") +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecipeIngredient)) return false;
        RecipeIngredient other = (RecipeIngredient) o;
        return this.recipeId     == other.recipeId &&
               this.ingredientId == other.ingredientId;
    }

    @Override
    public int hashCode() {
        return 31 * recipeId + ingredientId;
    }
}