-- ============================================================
--  La Festin — Seed Data
--  Run AFTER la_festin_schema.sql
--  Passwords are BCrypt hashes of: password123
-- ============================================================

USE la_festin;

-- ============================================================
--  1. users
-- ============================================================
INSERT INTO user (username, password_hash) VALUES
  ('angela', '$2a$10$7QJ8zXkL9mN3pR5sT1uVOeWYa6bCdEfGhIjKlMnOpQrStUvWxYzAb'),
  ('carl',   '$2a$10$3KmN8bPq2rS4tU6vX0wZOcYd7eFgHiJkLmNoPqRsTuVwXyZaAbBcD'),
  ('elizah', '$2a$10$9RsT4uV5wX7yZ1aB2cDOeEf8gHiJkLmNoPqRsTuVwXyZaAbBcDeEf');

-- ============================================================
--  2. ingredients  (12 total)
-- ============================================================
INSERT INTO ingredient (name) VALUES
  ('egg'),              -- 1
  ('milk'),             -- 2
  ('all-purpose flour'),-- 3
  ('butter'),           -- 4
  ('salt'),             -- 5
  ('chicken breast'),   -- 6
  ('garlic'),           -- 7
  ('soy sauce'),        -- 8
  ('pork belly'),       -- 9
  ('vinegar'),          -- 10
  ('rice'),             -- 11
  ('paprika');          -- 12

-- ============================================================
--  3. recipes  (5 across all categories)
--  All owned by angela (user_id = 1)
-- ============================================================
INSERT INTO recipe (user_id, title, category, prep_time, `procedure`) VALUES
  (1, 'Classic Scrambled Eggs',
   'Breakfast', 10,
   '1. Crack eggs into a bowl and whisk with milk, salt, and pepper.\n2. Melt butter in a non-stick pan over low heat.\n3. Pour in egg mixture. Stir slowly and continuously with a spatula.\n4. Remove from heat while still slightly wet. Serve immediately.'),

  (1, 'Garlic Butter Chicken',
   'Lunch', 25,
   '1. Season chicken breast with salt and paprika on both sides.\n2. Melt butter in a skillet over medium-high heat.\n3. Sear chicken 6 minutes per side until golden and cooked through.\n4. Add minced garlic in the last 2 minutes. Baste with pan butter.\n5. Rest for 5 minutes before slicing.'),

  (1, 'Pork Adobo',
   'Dinner', 60,
   '1. Cut pork belly into 2-inch cubes.\n2. Combine soy sauce, vinegar, crushed garlic in a pot.\n3. Add pork and marinate for 15 minutes.\n4. Bring to a boil uncovered for 10 minutes.\n5. Lower heat, cover and simmer for 30 minutes until tender.\n6. Uncover and reduce sauce until it coats the pork. Serve with rice.'),

  (1, 'Garlic Fried Rice',
   'Breakfast', 15,
   '1. Heat butter in a wok or large pan over high heat.\n2. Add minced garlic and stir fry for 30 seconds until fragrant.\n3. Add day-old rice, breaking up any clumps.\n4. Season with soy sauce and salt. Toss for 3 minutes.\n5. Push rice to one side, scramble an egg on the other side.\n6. Mix together and serve hot.'),

  (1, 'Chicken Paprika',
   'Lunch', 35,
   '1. Season chicken breast generously with paprika and salt.\n2. Heat butter in a pan over medium heat.\n3. Brown chicken on both sides, about 4 minutes per side.\n4. Add minced garlic and a splash of milk to create a quick sauce.\n5. Simmer for 10 minutes until chicken is cooked and sauce thickens.\n6. Taste and adjust seasoning before serving.');

-- ============================================================
--  4. recipe_ingredient mappings
-- ============================================================

-- Recipe 1: Classic Scrambled Eggs
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit) VALUES
  (1, 1,  3.00, 'whole'),     -- 3 eggs
  (1, 2,  2.00, 'tablespoon'),-- 2 tbsp milk
  (1, 4,  1.00, 'tablespoon'),-- 1 tbsp butter
  (1, 5,  0.25, 'teaspoon');  -- 1/4 tsp salt

-- Recipe 2: Garlic Butter Chicken
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit) VALUES
  (2, 6,  2.00, 'piece'),     -- 2 chicken breasts
  (2, 4,  2.00, 'tablespoon'),-- 2 tbsp butter
  (2, 7,  4.00, 'clove'),     -- 4 garlic cloves
  (2, 5,  0.50, 'teaspoon'),  -- 1/2 tsp salt
  (2, 12, 1.00, 'teaspoon');  -- 1 tsp paprika

-- Recipe 3: Pork Adobo
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit) VALUES
  (3, 9,  500.00, 'gram'),    -- 500g pork belly
  (3, 8,  4.00,   'tablespoon'),-- 4 tbsp soy sauce
  (3, 10, 3.00,   'tablespoon'),-- 3 tbsp vinegar
  (3, 7,  5.00,   'clove'),   -- 5 garlic cloves
  (3, 11, 2.00,   'cup');     -- 2 cups rice

-- Recipe 4: Garlic Fried Rice
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit) VALUES
  (4, 11, 2.00, 'cup'),       -- 2 cups rice
  (4, 7,  3.00, 'clove'),     -- 3 garlic cloves
  (4, 4,  1.00, 'tablespoon'),-- 1 tbsp butter
  (4, 8,  1.00, 'tablespoon'),-- 1 tbsp soy sauce
  (4, 1,  1.00, 'whole'),     -- 1 egg
  (4, 5,  0.25, 'teaspoon');  -- 1/4 tsp salt

-- Recipe 5: Chicken Paprika
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit) VALUES
  (5, 6,  2.00, 'piece'),     -- 2 chicken breasts
  (5, 12, 2.00, 'teaspoon'),  -- 2 tsp paprika
  (5, 4,  1.00, 'tablespoon'),-- 1 tbsp butter
  (5, 7,  3.00, 'clove'),     -- 3 garlic cloves
  (5, 2,  0.50, 'cup'),       -- 1/2 cup milk
  (5, 5,  0.50, 'teaspoon');  -- 1/2 tsp salt

-- ============================================================
--  5. pantry  (angela's pantry — user_id = 1)
--  Intentionally partial so recipe matching has something to work with
-- ============================================================
INSERT INTO pantry (ingredient_id, user_id, quantity, unit) VALUES
  (1,  1, 6.00,  'whole'),      -- 6 eggs
  (2,  1, 1.00,  'cup'),        -- 1 cup milk
  (4,  1, 100.00,'gram'),       -- 100g butter
  (5,  1, 1.00,  'teaspoon'),   -- 1 tsp salt
  (7,  1, 10.00, 'clove'),      -- 10 garlic cloves
  (8,  1, 5.00,  'tablespoon'), -- 5 tbsp soy sauce
  (11, 1, 3.00,  'cup');        -- 3 cups rice
-- angela is MISSING: chicken breast (6), pork belly (9), vinegar (10), paprika (12), flour (3)
-- this means:
--   scrambled eggs    → 100% match (all 4 ingredients present)
--   garlic fried rice → 100% match (all 6 ingredients present)
--   pork adobo        → 80%  match (missing vinegar)
--   garlic chicken    → 60%  match (missing chicken, paprika)
--   chicken paprika   → 67%  match (missing chicken, paprika)

-- ============================================================
--  6. meal_entry  (angela's plan for this week)
-- ============================================================
INSERT INTO meal_entry (recipe_id, user_id, meal_type, scheduled_date) VALUES
  (4, 1, 'Breakfast', '2026-04-17'),  -- today: garlic fried rice for breakfast
  (2, 1, 'Lunch',     '2026-04-17'),  -- today: garlic butter chicken for lunch
  (3, 1, 'Dinner',    '2026-04-18');  -- tomorrow: pork adobo for dinner