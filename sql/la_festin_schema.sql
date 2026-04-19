-- ============================================================
--  Le Festin — Full Schema Setup
--  Safe to re-run: drops and recreates all tables cleanly.
--  Run with:
--  mysql -u root -p < sql/la_festin_schema.sql
-- ============================================================

-- ── Database ──────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS la_festin
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE la_festin;

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
SELECT 'la_festin schema created successfully.' AS status;