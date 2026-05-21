# Le Festin 🍽️

**Recipe Database and Meal Planning System**

Le Festin is a desktop application designed for personalized recipe management and meal planning. Each user maintains an isolated collection of recipes, pantry inventory, scheduled meals, and even grocery list. The application features a Java Swing interface with a retro design aesthetic.

## Features

* **Pantry Management:** Track available ingredients by quantity and unit for specific users.

* **Recipe Matching:** Suggests recipes based on currently available pantry items.

* **Meal Planning:** Organize recipes into structured daily, weekly, or monthly plans.

* **Security:** Personal credentials and passwords are secured using BCrypt hashing.

* **Data Integrity:** Relational database normalized to 3NF to ensure no partial or transitive dependencies.

* **Unit Normalization:** Quantities are normalized before SQL persistence so pantry, recipe matching, and grocery calculations can compare units consistently.

## Prerequisites
* **JDK 17+**: The project uses Java 17 features.
* **MySQL Server 8.4+**: To host the application database.

## Getting Started

On first launch, Le Festin opens a **Setup Wizard** that guides you through entering your MySQL credentials, creating the schema, and loading seed data — no manual configuration needed.

### Option 1: Local Development (Build from Source)

Use this option if you want to run the app directly from the code.

1. **Clone the repository**
```bash
git clone https://github.com/ausdotsn50/le-festin.git
cd le-festin/le_festin
```

2. **Compile**

*Windows*
```bash
javac -cp "lib/*" -d out src\*.java src\config\*.java src\dao\*.java src\dao\impl\*.java src\helper\Helper.java src\model\*.java src\service\*.java src\ui\*.java src\ui\dialogs\*.java src\ui\panels\*.java
```

*MacOS*
```bash
javac -cp "lib/*" -d out src/*.java src/config/*.java src/dao/*.java src/dao/impl/*.java src/helper/Helper.java src/model/*.java src/service/*.java src/ui/*.java src/ui/dialogs/*.java src/ui/panels/*.java
```

3. **Run**

*Windows*
```bash
java -cp "out;lib/*" Main
```

*MacOS*
```bash
java -cp "out:lib/*" Main
```

The Setup Wizard will appear on first run — enter your MySQL connection details and follow the prompts to initialize the database.

## Running the Executable

1. Download and extract the files
2. Verify directory structure. Ensure the extracted folder contains the following files side-by-side. *Do not separate or move these files*, as the executable relies on the bundled jre folder to launch successfully.

```
ReleaseFolder/
├── jre/                # Bundled Java Runtime Environment
├── le-festin.jar       # The core Java application
└── le-festin.exe       # The Windows executable launcher
```
3. Set-up config.properties
4. Launch the application by double-clicking le-festin.exe

## Technical Architecture
Relational Schema 

```sql
user(user_id, username, password_hash)
PK: user_id

ingredient(ingredient_id, name)
PK: ingredient_id

recipe(recipe_id, user_id, title, category, prep_time, procedure)
PK: recipe_id
FK: user_id → user(user_id)

recipe_ingredient(recipe_id, ingredient_id, quantity, unit)
PK: recipe_id, ingredient_id
FK: recipe_id → recipe(recipe_id), ingredient_id → ingredient(ingredient_id)

pantry(ingredient_id, user_id, quantity, unit)
PK: ingredient_id, user_id
FK: ingredient_id → ingredient(ingredient_id), user_id → user(user_id)

meal_entry(user_id, meal_type, scheduled_date, recipe_id)
PK: user_id, scheduled_date, meal_type
FK: recipe_id → recipe(recipe_id), user_id → user(user_id)
```

## Project Structure

```text
📦 le-festin
 ┣ 📂 lib                            # Local dependency JARs (no download needed)
 ┃ ┣ 📜 commons-csv-1.10.0.jar
 ┃ ┣ 📜 jbcrypt-0.4.jar
 ┃ ┗ 📜 mysql-connector-j-9.6.0.jar
 ┣ 📂 resources
 ┃ ┣ 📜 config.properties            # Active credentials (gitignored)
 ┃ ┗ 📂 sql
 ┃   ┣ 📜 le_festin_schema.sql       # Database table definitions
 ┃   ┗ 📜 le_festin_seed.sql         # Initial data for recipes/ingredients
 ┣ 📂 src
 ┃ ┣ 📂 config                       # DB Connection & Setup Utilities
 ┃ ┣ 📂 dao                          # Data Access Objects (Interfaces & Impl)
 ┃ ┣ 📂 helper                       # Utility helpers
 ┃ ┣ 📂 model                        # Entity POJOs (User, Recipe, etc.)
 ┃ ┣ 📂 service                      # Business Logic (Auth, Matching, CSV)
 ┃ ┣ 📂 ui                           # Swing GUI (Frames, Panels, Dialogs)
 ┃ ┗ 📜 Main.java                    # App Entry Point
 ┣ 📂 installers                      # Distribution artifacts
 ┃ ┗ 📜 le-festin.jar               # Executable JAR for distribution
 ┣ 📜 .gitignore                     # Excludes config.properties and out/
 ┣ 📜 README.md                      # Project documentation
 ┣ 📜 config.properties.example      # Template for environment setup
 ┣ 📜 manifest.txt                   # JAR manifest (defines Main-Class)
 ┗ 📜 Makefile                       # Build targets: compile, jar, run, clean
```

## Contributors
* Angela Almazan 
* Carl Rodriguez 
* Elizah Sumbeling
