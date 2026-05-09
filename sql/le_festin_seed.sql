USE le_festin;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE meal_entry;
TRUNCATE TABLE pantry;
TRUNCATE TABLE recipe_ingredient;
TRUNCATE TABLE recipe;
TRUNCATE TABLE ingredient;
TRUNCATE TABLE user;

INSERT INTO ingredient (name) VALUES
    ('egg'),                -- 1
    ('milk'),               -- 2
    ('all-purpose flour'),  -- 3
    ('butter'),             -- 4
    ('salt'),               -- 5
    ('chicken breast'),     -- 6
    ('garlic'),             -- 7
    ('soy sauce'),          -- 8
    ('pork belly'),         -- 9
    ('vinegar'),            -- 10
    ('rice'),               -- 11
    ('paprika'),            -- 12
    ('sugar'),              -- 13
    ('olive oil'),          -- 14
    ('onion'),              -- 15
    ('tomato'),             -- 16
    ('cheese'),             -- 17
    ('bread');              -- 18

INSERT INTO recipe
    (user_id, title, category, prep_time, `procedure`) VALUES

    (1, 'Classic Scrambled Eggs',
     'Breakfast', 10,
     '1. Crack 3 eggs into a bowl. Whisk with milk and salt.\n2. Melt butter in a non-stick pan over low heat.\n3. Pour in egg mixture. Stir slowly until just set.\n4. Serve immediately.'),

    (1, 'Garlic Fried Rice',
     'Breakfast', 15,
     '1. Heat butter in a wok over high heat.\n2. Fry minced garlic 30 seconds.\n3. Add day-old rice and break up clumps.\n4. Season with soy sauce and salt.\n5. Push to side, scramble one egg, mix together.'),

    (1, 'French Toast',
     'Breakfast', 12,
     '1. Whisk eggs, milk, and sugar together.\n2. Dip bread slices in egg mixture.\n3. Melt butter in pan over medium heat.\n4. Cook each slice 2 minutes per side until golden.\n5. Serve with sugar dusted on top.'),

    (1, 'Garlic Butter Chicken',
     'Lunch', 25,
     '1. Season chicken breast with salt and paprika.\n2. Melt butter in skillet over medium-high heat.\n3. Sear chicken 6 minutes per side until golden.\n4. Add minced garlic in the last 2 minutes.\n5. Rest 5 minutes before slicing.'),

    (1, 'Tomato and Egg Stir Fry',
     'Lunch', 15,
     '1. Beat eggs with a pinch of salt.\n2. Heat olive oil in a wok over medium-high heat.\n3. Scramble eggs until just set, remove and set aside.\n4. In the same wok, stir-fry sliced tomatoes with sugar and salt.\n5. Return eggs, toss together and serve over rice.'),

    (1, 'Pork Adobo',
     'Dinner', 60,
     '1. Cut pork belly into 2-inch cubes.\n2. Combine soy sauce, vinegar, and garlic in a pot.\n3. Add pork, marinate 15 minutes.\n4. Boil uncovered 10 minutes.\n5. Lower heat, cover and simmer 30 minutes.\n6. Uncover and reduce sauce. Serve with rice.'),

    (1, 'Chicken Paprika',
     'Dinner', 35,
     '1. Season chicken with paprika and salt.\n2. Heat butter in pan over medium heat.\n3. Brown chicken 4 minutes per side.\n4. Add garlic and milk to make a quick sauce.\n5. Simmer 10 minutes until chicken is cooked through.'),

    (1, 'Cheesy Garlic Bread',
     'Snack', 10,
     '1. Slice bread and place on baking tray.\n2. Mix softened butter with minced garlic.\n3. Spread garlic butter on each slice.\n4. Top generously with cheese.\n5. Bake at 180C for 8 minutes until cheese melts.'),

    (3, 'Buttery Pancakes',
     'Dessert', 20,
     '1. Mix flour, egg, milk, sugar, and a pinch of salt.\n2. Melt butter and stir into batter.\n3. Heat pan over medium heat, lightly butter.\n4. Pour small ladles of batter.\n5. Cook until bubbles form, flip and cook 1 more minute.'),

    (3, 'Simple Onion Omelette',
     'Lunch', 10,
     '1. Dice onion finely.\n2. Beat 2 eggs with salt.\n3. Heat olive oil in pan, saute onion until soft.\n4. Pour in egg mixture.\n5. Fold omelette when edges set. Serve hot.');


INSERT INTO recipe_ingredient VALUES
    (1, 1,  3.00, 'whole'),       -- egg
    (1, 2,  2.00, 'tablespoon'),  -- milk
    (1, 4,  1.00, 'tablespoon'),  -- butter
    (1, 5,  0.25, 'teaspoon');    -- salt

INSERT INTO recipe_ingredient VALUES
    (2, 11, 2.00, 'cup'),         -- rice
    (2, 7,  3.00, 'clove'),       -- garlic
    (2, 4,  1.00, 'tablespoon'),  -- butter
    (2, 8,  1.00, 'tablespoon'),  -- soy sauce
    (2, 1,  1.00, 'whole'),       -- egg
    (2, 5,  0.25, 'teaspoon');    -- salt

INSERT INTO recipe_ingredient VALUES
    (3, 1,  2.00, 'whole'),       -- egg
    (3, 2,  0.25, 'cup'),         -- milk
    (3, 13, 1.00, 'tablespoon'),  -- sugar
    (3, 18, 4.00, 'slice'),       -- bread
    (3, 4,  1.00, 'tablespoon');  -- butter

INSERT INTO recipe_ingredient VALUES
    (4, 6,  2.00, 'piece'),       -- chicken breast
    (4, 4,  2.00, 'tablespoon'),  -- butter
    (4, 7,  4.00, 'clove'),       -- garlic
    (4, 5,  0.50, 'teaspoon'),    -- salt
    (4, 12, 1.00, 'teaspoon');    -- paprika

INSERT INTO recipe_ingredient VALUES
    (5, 1,  3.00, 'whole'),       -- egg
    (5, 16, 2.00, 'whole'),       -- tomato
    (5, 14, 2.00, 'tablespoon'),  -- olive oil
    (5, 13, 1.00, 'teaspoon'),    -- sugar
    (5, 5,  0.25, 'teaspoon');    -- salt

INSERT INTO recipe_ingredient VALUES
    (6, 9,  500.00, 'gram'),      -- pork belly
    (6, 8,  4.00,   'tablespoon'),-- soy sauce
    (6, 10, 3.00,   'tablespoon'),-- vinegar
    (6, 7,  5.00,   'clove'),     -- garlic
    (6, 11, 2.00,   'cup');       -- rice

INSERT INTO recipe_ingredient VALUES
    (7, 6,  2.00, 'piece'),       -- chicken breast
    (7, 12, 2.00, 'teaspoon'),    -- paprika
    (7, 4,  1.00, 'tablespoon'),  -- butter
    (7, 7,  3.00, 'clove'),       -- garlic
    (7, 2,  0.50, 'cup'),         -- milk
    (7, 5,  0.50, 'teaspoon');    -- salt

INSERT INTO recipe_ingredient VALUES
    (8, 18, 6.00, 'slice'),       -- bread
    (8, 4,  2.00, 'tablespoon'),  -- butter
    (8, 7,  3.00, 'clove'),       -- garlic
    (8, 17, 1.00, 'cup');         -- cheese

INSERT INTO recipe_ingredient VALUES
    (9, 3,  1.00, 'cup'),         -- all-purpose flour
    (9, 1,  1.00, 'whole'),       -- egg
    (9, 2,  0.75, 'cup'),         -- milk
    (9, 13, 2.00, 'tablespoon'),  -- sugar
    (9, 4,  2.00, 'tablespoon'),  -- butter
    (9, 5,  0.25, 'teaspoon');    -- salt

INSERT INTO recipe_ingredient VALUES
    (10, 1,  2.00, 'whole'),      -- egg
    (10, 15, 1.00, 'whole'),      -- onion
    (10, 14, 1.00, 'tablespoon'), -- olive oil
    (10, 5,  0.25, 'teaspoon');   -- salt

INSERT INTO pantry VALUES
    (1,  1, 12.00, 'whole'),       -- egg
    (2,  1,  2.00, 'cup'),         -- milk
    (3,  1,  2.00, 'cup'),         -- all-purpose flour
    (4,  1, 200.00,'gram'),        -- butter
    (5,  1,  3.00, 'teaspoon'),    -- salt
    (7,  1, 15.00, 'clove'),       -- garlic
    (8,  1,  8.00, 'tablespoon'),  -- soy sauce
    (11, 1,  5.00, 'cup'),         -- rice
    (13, 1,  4.00, 'tablespoon'),  -- sugar
    (14, 1,  4.00, 'tablespoon'),  -- olive oil
    (16, 1,  3.00, 'whole'),       -- tomato
    (18, 1, 12.00, 'slice');       -- bread

INSERT INTO pantry VALUES
    (1,  3,  4.00, 'whole'),       -- egg
    (14, 3,  2.00, 'tablespoon'),  -- olive oil
    (5,  3,  1.00, 'teaspoon');    -- salt

INSERT INTO meal_entry VALUES
    (1, 1, 'Breakfast', CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY),
    (4, 1, 'Lunch',     CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY),
    (6, 1, 'Dinner',    CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY),

    (2, 1, 'Breakfast', CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 1 DAY),
    (5, 1, 'Lunch',     CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 1 DAY),
    (7, 1, 'Dinner',    CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 1 DAY),

    (3, 1, 'Breakfast', CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 2 DAY),
    (4, 1, 'Lunch',     CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 2 DAY),
    (6, 1, 'Dinner',    CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 2 DAY),

    (1, 1, 'Breakfast', CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 3 DAY),
    (5, 1, 'Lunch',     CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 3 DAY),
    (7, 1, 'Dinner',    CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 3 DAY),

    (2, 1, 'Breakfast', CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 4 DAY),
    (8, 1, 'Lunch',     CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 4 DAY),
    (6, 1, 'Dinner',    CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 4 DAY);

INSERT INTO meal_entry VALUES
    (1, 1, 'Breakfast', CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 7 DAY),
    (4, 1, 'Lunch',     CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 8 DAY),
    (6, 1, 'Dinner',    CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY + INTERVAL 9 DAY);

SET FOREIGN_KEY_CHECKS = 1;
