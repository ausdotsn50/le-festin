package com.lafestin.model;

/**
 * PantryItem — maps to the `pantry` table.
 *
 * pantry(ingredient_id FK, user_id FK, quantity, unit)
 *
 * Composite PK: (ingredient_id, user_id)
 * Represents one ingredient a specific user has in their virtual pantry.
 */
public class PantryItem {

    // Fields
    private int    ingredientId;
    private int    userId;
    private double quantity;
    private String unit;

    // Denormalized display field — NOT stored in DB 
    // Populated by JOIN in PantryDAO so the UI shows "garlic" not ID 7
    private String ingredientName;

    // Constructor: full (used when reading from ResultSet) 
    public PantryItem(int ingredientId, int userId,
                      double quantity, String unit) {
        this.ingredientId = ingredientId;
        this.userId       = userId;
        this.quantity     = quantity;
        this.unit         = unit;
    }

    // Constructor: with name (used when reading from JOIN query)
    public PantryItem(int ingredientId, int userId,
                      double quantity, String unit,
                      String ingredientName) {
        this(ingredientId, userId, quantity, unit);
        this.ingredientName = ingredientName;
    }

    public PantryItem() {}

    // Getters
    public int    getIngredientId()   { return ingredientId;   }
    public int    getUserId()         { return userId;         }
    public double getQuantity()       { return quantity;       }
    public String getUnit()           { return unit;           }
    public String getIngredientName() { return ingredientName; }

    // Setters
    public void setIngredientId(int ingredientId)     { this.ingredientId   = ingredientId;   }
    public void setUserId(int userId)                 { this.userId         = userId;         }
    public void setQuantity(double quantity)           { this.quantity       = quantity;       }
    public void setUnit(String unit)                   { this.unit           = unit;           }
    public void setIngredientName(String name)         { this.ingredientName = name;           }

    // Convenience: formatted display for PantryPanel JTable 
    // Strips trailing .0 for whole numbers: 6.0 → "6", 0.5 → "0.5"
    public String getFormattedQuantity() {
        return (quantity == Math.floor(quantity))
            ? String.valueOf((int) quantity)
            : String.valueOf(quantity);
    }

    @Override
    public String toString() {
        return "PantryItem{" +
            "ingredientId="  + ingredientId  +
            ", userId="      + userId        +
            ", quantity="    + quantity      +
            ", unit='"       + unit          + '\'' +
            (ingredientName != null
                ? ", name='" + ingredientName + '\''
                : "") +
            '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PantryItem)) return false;
        PantryItem other = (PantryItem) o;
        return this.ingredientId == other.ingredientId &&
               this.userId       == other.userId;
    }

    @Override
    public int hashCode() {
        return 31 * ingredientId + userId;
    }
}