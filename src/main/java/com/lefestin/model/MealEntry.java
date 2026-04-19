package com.lefestin.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MealEntry — maps to the `meal_entry` table.
 *
 * meal_entry(recipe_id FK, user_id FK, meal_type, scheduled_date)
 *
 * Composite PK: (user_id, scheduled_date, meal_type)
 * Enforces one recipe per meal slot per day per user.
 */
public class MealEntry {

    // Meal type constants — match the DB ENUM exactly
    public static final String BREAKFAST = "Breakfast";
    public static final String LUNCH = "Lunch";
    public static final String DINNER = "Dinner";
    public static final String[] MEAL_TYPES = { BREAKFAST, LUNCH, DINNER };

    // Fields
    private int       recipeId;
    private int       userId;
    private String    mealType;
    private LocalDate scheduledDate;

    // Denormalized display fields — NOT stored in DB
    // Populated by JOIN in MealEntryDAO so planners can render
    // recipe title and category without a second query
    private String recipeTitle;
    private String recipeCategory;

    // Constructor: full (used when reading from ResultSet) 
    public MealEntry(int recipeId, int userId,
                     String mealType, LocalDate scheduledDate) {
        this.recipeId      = recipeId;
        this.userId        = userId;
        this.mealType      = mealType;
        this.scheduledDate = scheduledDate;
    }

    // Constructor: with recipe info (used when reading from JOIN)
    public MealEntry(int recipeId, int userId,
                     String mealType, LocalDate scheduledDate,
                     String recipeTitle, String recipeCategory) {
        this(recipeId, userId, mealType, scheduledDate);
        this.recipeTitle    = recipeTitle;
        this.recipeCategory = recipeCategory;
    }

    public MealEntry() {}

    // Getters
    public int       getRecipeId()       { return recipeId;       }
    public int       getUserId()         { return userId;         }
    public String    getMealType()       { return mealType;       }
    public LocalDate getScheduledDate()  { return scheduledDate;  }
    public String    getRecipeTitle()    { return recipeTitle;    }
    public String    getRecipeCategory() { return recipeCategory; }

    // Setters
    public void setRecipeId(int recipeId)              { this.recipeId       = recipeId;       }
    public void setUserId(int userId)                  { this.userId         = userId;         }
    public void setMealType(String mealType)           { this.mealType       = mealType;       }
    public void setScheduledDate(LocalDate date)       { this.scheduledDate  = date;           }
    public void setRecipeTitle(String title)           { this.recipeTitle    = title;          }
    public void setRecipeCategory(String category)     { this.recipeCategory = category;       }

    // Convenience: formatted date for display 
    // "Thursday, April 17 2026"
    public String getFormattedDate() {
        return scheduledDate.format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy")
        );
    }

    // Shorthand of gFD
    public String getShortDate() {
        return scheduledDate.format(
            DateTimeFormatter.ofPattern("MMM d")
        );
    }

    public String getDisplayTitle() {
        return (recipeTitle != null && !recipeTitle.isBlank())
            ? recipeTitle
            : "(empty)";
    }

    @Override
    public String toString() {
        return "MealEntry{" +
            "recipeId="      + recipeId      +
            ", userId="      + userId        +
            ", mealType='"   + mealType      + '\'' +
            ", date="        + scheduledDate +
            (recipeTitle != null
                ? ", recipe='" + recipeTitle + '\''
                : "") +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MealEntry)) return false;
        MealEntry other = (MealEntry) o;
        return this.userId == other.userId
            && this.mealType.equals(other.mealType)
            && this.scheduledDate.equals(other.scheduledDate);
    }

    @Override
    public int hashCode() {
        int result = userId;
        result = 31 * result + mealType.hashCode();
        result = 31 * result + scheduledDate.hashCode();
        return result;
    }
}