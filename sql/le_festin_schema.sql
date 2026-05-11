-- ============================================================
--  Le Festin — Full Schema Setup
--  Safe to re-run: drops and recreates all tables cleanly.
--  Run with:
--  mysql -u root -p < sql/la_festin_schema.sql
-- ============================================================

-- ── Database ──────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS le_festin
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE le_festin;

-- ── Safety: drop in reverse dependency order ──────────────
-- Child tables first, parent tables last.
-- Prevents FK constraint errors on re-run.
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS meal_entry;
DROP TABLE IF EXISTS pantry;
DROP TABLE IF EXISTS recipe_ingredient;
DROP TABLE IF EXISTS recipe;
DROP TABLE IF EXISTS ingredient;
DROP TABLE IF EXISTS user;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
--  TABLE 1: user
--  No dependencies — created first.
-- ============================================================
-- Enforcing unique usernames
CREATE TABLE user (
    user_id       INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    CONSTRAINT pk_user     PRIMARY KEY (user_id),
    CONSTRAINT uq_username UNIQUE      (username)
);

-- ============================================================
--  TABLE 2: ingredient
--  No dependencies — created alongside user.
-- ============================================================
-- Enforcing unique ingredient names such that it does not get added to the DB if
-- there is an existing one

-- 
CREATE TABLE ingredient (
    ingredient_id INT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,

    CONSTRAINT pk_ingredient PRIMARY KEY (ingredient_id),
    CONSTRAINT uq_ingredient UNIQUE      (name)
);

-- ============================================================
--  TABLE 3: recipe
--  Depends on: user
-- ============================================================
CREATE TABLE recipe (
    recipe_id  INT         NOT NULL AUTO_INCREMENT,
    user_id    INT         NOT NULL,
    title      VARCHAR(150) NOT NULL,
    category   ENUM(
                 'Breakfast',
                 'Lunch',
                 'Dinner',
                 'Snack',
                 'Dessert'
               )            NOT NULL,
    prep_time  INT          NOT NULL,
    `procedure`  TEXT         NOT NULL,

    CONSTRAINT pk_recipe       PRIMARY KEY (recipe_id),
    CONSTRAINT fk_recipe_user  FOREIGN KEY (user_id)
        REFERENCES user (user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_prep_time   CHECK (prep_time > 0)
);

-- ============================================================
--  TABLE 4: recipe_ingredient
--  Depends on: recipe, ingredient
-- ============================================================
CREATE TABLE recipe_ingredient (
    recipe_id     INT            NOT NULL,
    ingredient_id INT            NOT NULL,
    quantity      DECIMAL(10, 2) NOT NULL,
    unit          VARCHAR(50)    NOT NULL,

    CONSTRAINT pk_recipe_ingredient
        PRIMARY KEY (recipe_id, ingredient_id),
    CONSTRAINT fk_ri_recipe
        FOREIGN KEY (recipe_id)
        REFERENCES recipe (recipe_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ri_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredient (ingredient_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_ri_quantity CHECK (quantity > 0)
);

-- ============================================================
--  TABLE 5: pantry
--  Depends on: ingredient, user
-- ============================================================
CREATE TABLE pantry (
    ingredient_id INT            NOT NULL,
    user_id       INT            NOT NULL,
    quantity      DECIMAL(10, 2) NOT NULL,
    unit          VARCHAR(50)    NOT NULL,

    CONSTRAINT pk_pantry PRIMARY KEY (ingredient_id, user_id),
    CONSTRAINT fk_pantry_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredient (ingredient_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_pantry_user
        FOREIGN KEY (user_id)
        REFERENCES user (user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_pantry_qty CHECK (quantity >= 0)
);

-- ============================================================
--  TABLE 6: meal_entry
--  Depends on: recipe, user
-- ============================================================
CREATE TABLE meal_entry (
    recipe_id      INT  NOT NULL,
    user_id        INT  NOT NULL,
    meal_type      ENUM(
                     'Breakfast',
                     'Lunch',
                     'Dinner'
                   )     NOT NULL,
    scheduled_date DATE NOT NULL,

    CONSTRAINT pk_meal_entry
        PRIMARY KEY (user_id, scheduled_date, meal_type),
    CONSTRAINT fk_me_recipe
        FOREIGN KEY (recipe_id)
        REFERENCES recipe (recipe_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_me_user
        FOREIGN KEY (user_id)
        REFERENCES user (user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ============================================================
--  INDEXES
-- ============================================================
CREATE INDEX idx_recipe_user  ON recipe          (user_id); 
CREATE INDEX idx_ri_recipe    ON recipe_ingredient(recipe_id);
CREATE INDEX idx_pantry_user  ON pantry           (user_id);
CREATE INDEX idx_me_user_date ON meal_entry       (user_id, scheduled_date);

-- ============================================================
--  DONE
-- ============================================================
SELECT 'le_festin schema created successfully.' AS status;





-- ============================================================
--  Le Festin — Unit Normalization Migration
--
--  Purpose:
--  Align existing pantry and recipe_ingredient rows with the
--  canonical storage units now used by ConversionService:
--    - MASS   -> gram
--    - VOLUME -> milliliter
--    - COUNT  -> piece
--
-- ============================================================

START TRANSACTION;

-- Recipe ingredients -> canonical units
UPDATE recipe_ingredient
SET
    quantity = ROUND(
        CASE LOWER(TRIM(unit))
            WHEN 'gram' THEN quantity
            WHEN 'grams' THEN quantity
            WHEN 'g' THEN quantity
            WHEN 'kilogram' THEN quantity * 1000
            WHEN 'kilograms' THEN quantity * 1000
            WHEN 'kg' THEN quantity * 1000

            WHEN 'milliliter' THEN quantity
            WHEN 'milliliters' THEN quantity
            WHEN 'ml' THEN quantity
            WHEN 'liter' THEN quantity * 1000
            WHEN 'liters' THEN quantity * 1000
            WHEN 'l' THEN quantity * 1000
            WHEN 'teaspoon' THEN quantity * 5
            WHEN 'teaspoons' THEN quantity * 5
            WHEN 'tsp' THEN quantity * 5
            WHEN 'tablespoon' THEN quantity * 15
            WHEN 'tablespoons' THEN quantity * 15
            WHEN 'tbsp' THEN quantity * 15
            WHEN 'cup' THEN quantity * 240
            WHEN 'cups' THEN quantity * 240

            ELSE quantity
        END, 2
    ),
    unit = CASE
        WHEN LOWER(TRIM(unit)) IN ('gram', 'grams', 'g', 'kilogram', 'kilograms', 'kg')
            THEN 'gram'
        WHEN LOWER(TRIM(unit)) IN (
            'milliliter', 'milliliters', 'ml',
            'liter', 'liters', 'l',
            'teaspoon', 'teaspoons', 'tsp',
            'tablespoon', 'tablespoons', 'tbsp',
            'cup', 'cups'
        )
            THEN 'milliliter'
        WHEN LOWER(TRIM(unit)) IN (
            'piece', 'pieces', 'whole', 'wholes',
            'clove', 'cloves', 'slice', 'slices',
            'pinch', 'pinches'
        )
            THEN 'piece'
        ELSE LOWER(TRIM(unit))
    END;

-- Pantry -> canonical units
UPDATE pantry
SET
    quantity = ROUND(
        CASE LOWER(TRIM(unit))
            WHEN 'gram' THEN quantity
            WHEN 'grams' THEN quantity
            WHEN 'g' THEN quantity
            WHEN 'kilogram' THEN quantity * 1000
            WHEN 'kilograms' THEN quantity * 1000
            WHEN 'kg' THEN quantity * 1000

            WHEN 'milliliter' THEN quantity
            WHEN 'milliliters' THEN quantity
            WHEN 'ml' THEN quantity
            WHEN 'liter' THEN quantity * 1000
            WHEN 'liters' THEN quantity * 1000
            WHEN 'l' THEN quantity * 1000
            WHEN 'teaspoon' THEN quantity * 5
            WHEN 'teaspoons' THEN quantity * 5
            WHEN 'tsp' THEN quantity * 5
            WHEN 'tablespoon' THEN quantity * 15
            WHEN 'tablespoons' THEN quantity * 15
            WHEN 'tbsp' THEN quantity * 15
            WHEN 'cup' THEN quantity * 240
            WHEN 'cups' THEN quantity * 240

            ELSE quantity
        END, 2
    ),
    unit = CASE
        WHEN LOWER(TRIM(unit)) IN ('gram', 'grams', 'g', 'kilogram', 'kilograms', 'kg')
            THEN 'gram'
        WHEN LOWER(TRIM(unit)) IN (
            'milliliter', 'milliliters', 'ml',
            'liter', 'liters', 'l',
            'teaspoon', 'teaspoons', 'tsp',
            'tablespoon', 'tablespoons', 'tbsp',
            'cup', 'cups'
        )
            THEN 'milliliter'
        WHEN LOWER(TRIM(unit)) IN (
            'piece', 'pieces', 'whole', 'wholes',
            'clove', 'cloves', 'slice', 'slices',
            'pinch', 'pinches'
        )
            THEN 'piece'
        ELSE LOWER(TRIM(unit))
    END;

COMMIT;

SELECT 'unit normalization migration completed' AS status;