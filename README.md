# la-festin

## Run the project w/ Maven
- mvn clean install        # downloads all deps + compiles
- mvn test                 # runs JUnit tests
- mvn package              # builds the fat JAR in /target

```
Main
mvn compile exec:java -Dexec.mainClass="com.lefestin.Main"
mvn compile exec:java "-Dexec.mainClass=com.lefestin.Main"

Seedtest
mvn compile exec:java -Dexec.mainClass="com.lefestin.config.SeedDataTest"
mvn compile exec:java "-Dexec.mainClass=com.lefestin.config.SeedDataTest"
```

## DAO
- RecipeDAO tested

## Project structure
```
la-festin/
├── pom.xml
├── config.properties.example
├── README.md
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── lafestin/
    │   │           │
    │   │           ├── Main.java                          ← app entry point
    │   │           │
    │   │           ├── config/
    │   │           │   ├── DBConnection.java              ← JDBC singleton
    │   │           │   └── ConfigLoader.java              ← reads config.properties
    │   │           │
    │   │           ├── model/
    │   │           │   ├── User.java
    │   │           │   ├── Recipe.java
    │   │           │   ├── Ingredient.java
    │   │           │   ├── RecipeIngredient.java
    │   │           │   ├── PantryItem.java
    │   │           │   ├── MealEntry.java
    │   │           │   └── RecipeMatchResult.java         ← used by matching service
    │   │           │
    │   │           ├── dao/
    │   │           │   ├── UserDAO.java
    │   │           │   ├── RecipeDAO.java
    │   │           │   ├── IngredientDAO.java
    │   │           │   ├── RecipeIngredientDAO.java
    │   │           │   ├── PantryDAO.java
    │   │           │   └── MealEntryDAO.java
    │   │           │
    │   │           ├── service/
    │   │           │   ├── AuthService.java               ← login/register + bcrypt
    │   │           │   ├── RecipeMatchingService.java     ← pantry % matching
    │   │           │   ├── GroceryListService.java        ← missing ingredients
    │   │           │   ├── MealPlanGeneratorService.java  ← auto-fill week slots
    │   │           │   └── CsvExportService.java          ← meal plan/grocery CSV
    │   │           │
    │   │           └── ui/
    │   │               ├── MainFrame.java                 ← JFrame shell + CardLayout
    │   │               │
    │   │               ├── panels/
    │   │               │   ├── RecipeListPanel.java
    │   │               │   ├── RecipeDetailPanel.java
    │   │               │   ├── PantryPanel.java
    │   │               │   ├── RecipeSuggestionsPanel.java
    │   │               │   ├── DayPlannerPanel.java
    │   │               │   ├── WeeklyPlannerPanel.java
    │   │               │   ├── MonthlyOverviewPanel.java
    │   │               │   └── GroceryListPanel.java
    │   │               │
    │   │               ├── dialogs/
    │   │               │   ├── AddEditRecipeDialog.java
    │   │               │   ├── AddEditIngredientDialog.java
    │   │               │   ├── AssignRecipeDialog.java
    │   │               │   └── LoginDialog.java
    │   │               │
    │   │               └── components/
    │   │                   ├── RoundedButton.java         ← reusable styled button
    │   │                   ├── RecipeMatchCard.java       ← single suggestion card
    │   │                   └── MealSlotCell.java          ← weekly grid cell
    │   │
    │   └── resources/
    │       ├── config.properties                          ← gitignored, real credentials
    │       └── logback.xml                               ← logging config
    │
    └── test/
        └── java/
            └── com/
                └── lafestin/
                    ├── dao/
                    │   ├── RecipeDAOTest.java
                    │   ├── PantryDAOTest.java
                    │   └── MealEntryDAOTest.java
                    └── service/
                        ├── RecipeMatchingServiceTest.java
                        └── GroceryListServiceTest.java
```
