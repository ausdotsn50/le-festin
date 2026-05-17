package com.lefestin.model;

/**
 * Recipe — maps directly to the `recipe` table.
 *
 * recipe(recipe_id, user_id, title, category, prep_time, procedure)
 */
public class Recipe {

    // Fields
    private int    recipeId;
    private int    userId;
    private String title;
    private String category;
    private int    prepTime;
    private String procedure;

    // Constants: valid category values matching the DB ENUM 
    public static final String CATEGORY_BREAKFAST = "Breakfast";
    public static final String CATEGORY_LUNCH = "Lunch";
    public static final String CATEGORY_DINNER = "Dinner";

    // Constructor: full (used when reading from ResultSet) 
    public Recipe(int recipeId, int userId, String title,
                  String category, int prepTime, String procedure) {
        this.recipeId  = recipeId;
        this.userId    = userId;
        this.title     = title;
        this.category  = category;
        this.prepTime  = prepTime;
        this.procedure = procedure;
    }

    // Constructor: no ID (used when inserting a new recipe) ─────────────
    // recipeId is left 0 — MySQL assigns it via AUTO_INCREMENT on INSERT
    public Recipe(int userId, String title,
                  String category, int prepTime, String procedure) {
        this(0, userId, title, category, prepTime, procedure);
    }

    public Recipe() {}

    // Getters
    public int    getRecipeId()  { return recipeId;  }
    public int    getUserId()    { return userId;    }
    public String getTitle()     { return title;     }
    public String getCategory()  { return category;  }
    public int    getPrepTime()  { return prepTime;  }
    public String getProcedure() { return procedure; }

    // Setters
    public void setRecipeId(int recipeId)    { this.recipeId  = recipeId;  }
    public void setUserId(int userId)        { this.userId    = userId;    }
    public void setTitle(String title)       { this.title     = title;     }
    public void setCategory(String category) { this.category  = category;  }
    public void setPrepTime(int prepTime)    { this.prepTime  = prepTime;  }
    public void setProcedure(String procedure){ this.procedure = procedure; }

    public String getFormattedPrepTime() {
        if (prepTime < 60) {
            return prepTime + " min";
        }
        int hours   = prepTime / 60;
        int minutes = prepTime % 60;
        return minutes == 0
            ? hours + " hr"
            : hours + " hr " + minutes + " min";
    }

    @Override
    public String toString() {
        return "Recipe{" +
            "recipeId="  + recipeId  +
            ", userId="  + userId    +
            ", title='"  + title     + '\'' +
            ", category='" + category + '\'' +
            ", prepTime=" + prepTime  +
            ", procedure='" + procedure.substring(0, Math.min(30, procedure.length())) + "...'" +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recipe)) return false;
        Recipe other = (Recipe) o;
        return this.recipeId == other.recipeId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(recipeId);
    }
}