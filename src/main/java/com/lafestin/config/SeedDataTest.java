package com.lafestin.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.lafestin.dao.IngredientDAO;
import com.lafestin.dao.RecipeDAO;
import com.lafestin.dao.RecipeIngredientDAO;
import com.lafestin.model.Ingredient;
import com.lafestin.model.Recipe;
import com.lafestin.model.RecipeIngredient;
import com.lafestin.service.AuthService;
import com.lafestin.service.AuthService.AuthResult;

/**
 * SeedDataTest — verifies JDBC is working AND seed data landed correctly.
 * Queries all 6 tables and prints a summary of every row.
 *
 * Run with:
 *   mvn compile exec:java -Dexec.mainClass="com.lafestin.config.SeedDataTest"
 *
 * Delete this file after confirming everything passes.
 */
public class SeedDataTest {

    public static void main(String[] args) {
        Connection conn = DBConnection.getInstance().getConnection();
        RecipeDAO dao = new RecipeDAO();
        RecipeIngredientDAO riDAO = new RecipeIngredientDAO();
        IngredientDAO ingredientDAO = new IngredientDAO();
        AuthService auth = new AuthService();

        printSeparator("La Festin — Seed Data Verification");

        /*
        testUsers(conn);
        testIngredients(conn);
        testRecipes(conn);
        testRecipeIngredients(conn);
        testPantry(conn);
        testMealEntries(conn);
         */

        // DAO tester
        // testRecipeDAO(dao);
        // testRecipeIngredientDAO(riDAO);
        // testIngredientDAO(ingredientDAO);

        // Service tester
        testAuth(conn, auth);

        printSeparator("ALL QUERIES COMPLETED");
        DBConnection.getInstance().close();
    }

    private static void testAuth(Connection conn, AuthService auth) {
        // Test 1 — register a new user
        AuthResult reg = auth.register("testchef", "kitchen99");
        System.out.println("register success: " + reg.isSuccess()); // true
        System.out.println("register message: " + reg.getMessage()); // Welcome...
        System.out.println("register userId:  " + reg.getUser().getUserId()); // 4+

        // Test 2 — duplicate username blocked
        AuthResult dup = auth.register("testchef", "other123");
        System.out.println("duplicate: " + dup.isSuccess());  // false
        System.out.println("dup msg:   " + dup.getMessage()); // already taken

        // Test 3 — login with correct password
        AuthResult login = auth.login("testchef", "kitchen99");
        System.out.println("login success: " + login.isSuccess()); // true
        System.out.println("login user:    " + login.getUser().getUsername()); // testchef

        // Test 4 — wrong password returns null user, same generic message
        AuthResult wrong = auth.login("testchef", "wrongpass");
        System.out.println("wrong pw success: " + wrong.isSuccess()); // false
        System.out.println("wrong pw msg:     " + wrong.getMessage()); // Invalid username or password

        // Test 5 — unknown username same generic message
        AuthResult unknown = auth.login("nobody", "password");
        System.out.println("unknown success: " + unknown.isSuccess()); // false
        System.out.println("unknown msg:     " + unknown.getMessage()); // Invalid username or password

        // Test 6 — validation catches short password
        AuthResult shortPw = auth.register("validuser", "abc");
        System.out.println("short pw: " + shortPw.getMessage()); // at least 6 characters

        // Test 7 — validation catches username with spaces
        AuthResult space = auth.register("bad user", "password123");
        System.out.println("space msg: " + space.getMessage()); // cannot contain spaces

        // Cleanup
        try {
            PreparedStatement del = conn.prepareStatement(
            "DELETE FROM user WHERE username = 'testchef'");
            del.executeUpdate();
            del.close();  
        } catch (SQLException e) {
            // TODO: handle exception
        }
    }

    private static void testIngredientDAO(IngredientDAO ingredientDAO) {
        try {
            // Test 1 — getAllIngredients, expect 12 ordered by name
            List<Ingredient> all = ingredientDAO.getAllIngredients();
            System.out.println("getAllIngredients: " + all.size()); // 12
            System.out.println("First:  " + all.get(0).getName()); // all-purpose flour
            System.out.println("Last:   " + all.get(all.size()-1).getName()); // vinegar

            // Test 2 — getIngredientById for garlic (id=7)
            Ingredient garlic = ingredientDAO.getIngredientById(7);
            System.out.println("getById: " + garlic.getName()); // garlic

            // Test 3 — searchByName partial match
            List<Ingredient> results = ingredientDAO.searchByName("gar");
            System.out.println("searchByName 'gar': " + results.size()); // 1 (garlic)

            // Test 4 — existsByName
            System.out.println("existsByName 'garlic': " + ingredientDAO.existsByName("garlic")); // true
            System.out.println("existsByName 'truffle': " + ingredientDAO.existsByName("truffle")); // false

            // Test 5 — findOrCreate: existing
            Ingredient found = ingredientDAO.findOrCreate("garlic");
            System.out.println("findOrCreate existing id: " + found.getIngredientId()); // 7

            // Test 6 — findOrCreate: new
            Ingredient created = ingredientDAO.findOrCreate("truffle");
            System.out.println("findOrCreate new id: " + created.getIngredientId()); // 13+

            // Cleanup test ingredient
            ingredientDAO.deleteIngredient(created.getIngredientId());
            System.out.println("After delete: " + ingredientDAO.getAllIngredients().size()); // 12
        } catch(SQLException e) {

        }
    }
    private static void testRecipeIngredientDAO(RecipeIngredientDAO riDAO) {
        try {
            // Test 1 — get ingredients for Pork Adobo (recipeId=3), expect 5 rows
            List<RecipeIngredient> ri = riDAO.getIngredientsByRecipeId(3);
            System.out.println("Pork Adobo ingredients: " + ri.size()); // 5

            // Print each row — confirms JOIN is working and name is populated
            for (RecipeIngredient r : ri) {
                System.out.println("  " + r.getFormattedAmount());
            }
            // Expected:
            //   5 clove garlic
            //   500 gram pork belly
            //   3 tablespoon soy sauce
            //   3 cup rice (ordered alphabetically)
            //   3 tablespoon vinegar

            // Test 2 — add/delete round trip
            RecipeIngredient newRi = new RecipeIngredient(1, 3, 2.0, "tablespoon");
            riDAO.addRecipeIngredient(newRi);
            List<RecipeIngredient> after = riDAO.getIngredientsByRecipeId(1);
            System.out.println("After add (recipe 1): " + after.size()); // 5 (was 4)

            riDAO.deleteByRecipeAndIngredient(1, 3);
            List<RecipeIngredient> afterDelete = riDAO.getIngredientsByRecipeId(1);
            System.out.println("After delete (recipe 1): " + afterDelete.size()); // 4

            // Test 3 — deleteByRecipeId wipes all rows for a recipe
            // (use a throwaway recipe so seed data is not affected)
        } catch(SQLException e) {

        }
    }

    private static void testRecipeDAO(RecipeDAO dao) {
        try {
            // Test 1 — getAllRecipes for angela (userId=1), expect 5 rows
            List<Recipe> all = dao.getAllRecipes(1);
            System.out.println("getAllRecipes: " + all.size()); // 5

            // Test 2 — getRecipeById for Pork Adobo (recipeId=3)
            Recipe r = dao.getRecipeById(3);
            System.out.println("getRecipeById: " + r.getTitle()); // Pork Adobo

            // Test 3 — searchRecipes by keyword "garlic", expect 2 results
            List<Recipe> garlic = dao.searchRecipes(1, "garlic", null);
            System.out.println("searchRecipes garlic: " + garlic.size()); // 2

            // Test 4 — searchRecipes by category Breakfast, expect 2 results
            List<Recipe> breakfast = dao.searchRecipes(1, null, "Breakfast");
            System.out.println("searchRecipes Breakfast: " + breakfast.size()); // 2

            // Test 5 — add, update, delete round trip
            Recipe newRecipe = new Recipe(1, "Test Soup", "Dinner", 20, "Boil water.");
            dao.addRecipe(newRecipe);
            System.out.println("addRecipe id: " + newRecipe.getRecipeId()); // 6 or next id

            newRecipe.setTitle("Updated Soup");
            dao.updateRecipe(newRecipe);
            Recipe updated = dao.getRecipeById(newRecipe.getRecipeId());
            System.out.println("updateRecipe: " + updated.getTitle()); // Updated Soup

            dao.deleteRecipe(newRecipe.getRecipeId());
            Recipe deleted = dao.getRecipeById(newRecipe.getRecipeId());
            System.out.println("deleteRecipe: " + deleted); // null
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ── 1. users ──────────────────────────────────────────────────────────
    private static void testUsers(Connection conn) {
        printSection("1. user table");
        String sql = "SELECT user_id, username FROM user ORDER BY user_id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                System.out.printf("  [%d] %s%n",
                    rs.getInt("user_id"),
                    rs.getString("username"));
                count++;
            }
            printResult(count, 3, "users");
        } catch (SQLException e) {
            printError("user", e);
        }
    }

    // ── 2. ingredients ────────────────────────────────────────────────────
    private static void testIngredients(Connection conn) {
        printSection("2. ingredient table");
        String sql = "SELECT ingredient_id, name FROM ingredient ORDER BY ingredient_id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                System.out.printf("  [%d] %s%n",
                    rs.getInt("ingredient_id"),
                    rs.getString("name"));
                count++;
            }
            printResult(count, 12, "ingredients");
        } catch (SQLException e) {
            printError("ingredient", e);
        }
    }

    // ── 3. recipes ────────────────────────────────────────────────────────
    private static void testRecipes(Connection conn) {
        printSection("3. recipe table");
        String sql = """
            SELECT recipe_id, title, category, prep_time
            FROM recipe
            ORDER BY recipe_id
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                System.out.printf("  [%d] %-30s | %-10s | %d min%n",
                    rs.getInt("recipe_id"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getInt("prep_time"));
                count++;
            }
            printResult(count, 5, "recipes");
        } catch (SQLException e) {
            printError("recipe", e);
        }
    }

    // ── 4. recipe_ingredients ─────────────────────────────────────────────
    private static void testRecipeIngredients(Connection conn) {
        printSection("4. recipe_ingredient table (with JOIN)");
        String sql = """
            SELECT r.title, i.name, ri.quantity, ri.unit
            FROM recipe_ingredient ri
            JOIN recipe     r ON ri.recipe_id     = r.recipe_id
            JOIN ingredient i ON ri.ingredient_id = i.ingredient_id
            ORDER BY r.recipe_id, i.name
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs   = stmt.executeQuery();
            String lastRecipe = "";
            int count = 0;
            while (rs.next()) {
                String recipe = rs.getString("title");
                if (!recipe.equals(lastRecipe)) {
                    System.out.println("  " + recipe + ":");
                    lastRecipe = recipe;
                }
                System.out.printf("      %-10s %-15s %s%n",
                    rs.getString("quantity"),
                    rs.getString("unit"),
                    rs.getString("name"));
                count++;
            }
            printResult(count, 21, "recipe_ingredient rows");
        } catch (SQLException e) {
            printError("recipe_ingredient", e);
        }
    }

    // ── 5. pantry ─────────────────────────────────────────────────────────
    private static void testPantry(Connection conn) {
        printSection("5. pantry table (angela's pantry)");
        String sql = """
            SELECT u.username, i.name, p.quantity, p.unit
            FROM pantry p
            JOIN user       u ON p.user_id       = u.user_id
            JOIN ingredient i ON p.ingredient_id = i.ingredient_id
            WHERE u.username = 'angela'
            ORDER BY i.name
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                System.out.printf("  %-10s %-10s %s%n",
                    rs.getString("quantity"),
                    rs.getString("unit"),
                    rs.getString("name"));
                count++;
            }
            printResult(count, 7, "pantry items");
        } catch (SQLException e) {
            printError("pantry", e);
        }
    }

    // ── 6. meal_entries ───────────────────────────────────────────────────
    private static void testMealEntries(Connection conn) {
        printSection("6. meal_entry table (with JOIN)");
        String sql = """
            SELECT me.scheduled_date, me.meal_type, r.title, u.username
            FROM meal_entry me
            JOIN recipe r ON me.recipe_id = r.recipe_id
            JOIN user   u ON me.user_id   = u.user_id
            ORDER BY me.scheduled_date, me.meal_type
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                System.out.printf("  %s | %-10s | %-30s | %s%n",
                    rs.getDate("scheduled_date"),
                    rs.getString("meal_type"),
                    rs.getString("title"),
                    rs.getString("username"));
                count++;
            }
            printResult(count, 3, "meal entries");
        } catch (SQLException e) {
            printError("meal_entry", e);
        }
    }

    // ── Print helpers ─────────────────────────────────────────────────────
    private static void printSeparator(String title) {
        System.out.println();
        System.out.println("═".repeat(55));
        System.out.println("  " + title);
        System.out.println("═".repeat(55));
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println("  ── " + title + " ──");
    }

    private static void printResult(int actual, int expected, String label) {
        boolean ok = actual == expected;
        System.out.printf("%n  %s %d %s (expected %d)%n",
            ok ? "✓" : "✗",
            actual, label, expected);
        if (!ok) {
            System.err.println("  MISMATCH — did you run la_festin_seed.sql?");
        }
    }

    private static void printError(String table, SQLException e) {
        System.err.println("  ✗ Failed to query " + table + ": "
            + e.getMessage());
    }
}