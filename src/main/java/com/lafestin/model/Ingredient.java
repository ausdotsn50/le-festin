package com.lafestin.model;

/**
 * Ingredient — maps to the `ingredient` table.
 *
 * ingredient(ingredient_id, name)
 *
 * Shared across recipes and pantry — not user-scoped.
 * A global ingredient list that every user draws from.
 */
public class Ingredient {

    // Fields
    private int    ingredientId;
    private String name;

    // Constructor: full (used when reading from ResultSet) 
    public Ingredient(int ingredientId, String name) {
        this.ingredientId = ingredientId;
        this.name         = name;
    }

    // Constructor: no ID (used when inserting a new ingredient) 
    public Ingredient(String name) {
        this(0, name);
    }

    public Ingredient() {}

    // Getters
    public int    getIngredientId() { return ingredientId; }
    public String getName()         { return name;         }

    // Setters
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
    public void setName(String name)               { this.name = name;                }

    @Override
    public String toString() {
        return "Ingredient{" +
            "ingredientId=" + ingredientId +
            ", name='"      + name         + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ingredient)) return false;
        Ingredient other = (Ingredient) o;
        return this.ingredientId == other.ingredientId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(ingredientId);
    }
}