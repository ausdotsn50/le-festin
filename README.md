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
* **Maven**: For dependency and build management.
* **MySQL Server 8.4+**: To host the application database.

## Getting Started
### Option 1: Local Development (Build from Source)

Use this option if you want to run the app directly from the code. The included Maven utilities will handle the database setup for you.

1. **Clone the repository**
```bash
git clone https://github.com/ausdotsn50/le-festin.git
cd le-festin/
```

2. **Application Environment**:
Rename config.properties.example to src/main/resources/config.properties and update your credentials:

```properties
db.url=jdbc:mysql://localhost:3306/
db.database=le_festin
db.user=your_username
db.password=your_password
```

3. **Install & initialize**
```bash
# Step 1: Install dependencies to local repository
mvn clean install

# Step 2: Create schema, seed data, and fix credentials
mvn exec:java "-Dexec.mainClass=com.lefestin.config.SetupDatabase"
mvn exec:java "-Dexec.mainClass=com.lefestin.config.FixSeedPasswords"
```

4. **Launch the app**
```bash
mvn compile exec:java "-Dexec.mainClass=com.lefestin.Main"
```

## Option 2: Running the Executable (JAR Version)
Use this option for a standalone demonstration. This requires manual database and environment configuration.

1. **Download the JAR**
- Navigate to the [GitHub Repository](https://github.com/ausdotsn50/le-festin)
- Locate the file le-festin-1.0-SNAPSHOT-fat.jar in the root folder.
- Click on the file name, then click the Download button in the header [...] to save it to your machine.


2. **Database Setup**
- Import the schema: mysql -u root -p < sql/le_festin_schema.sql.
- Import seeds: mysql -u root -p < sql/le_festin_seed.sql.

3. **Execute**
Open your terminal in the folder where you downloaded the JAR and run:

```bash
java -jar le-festin-1.0-SNAPSHOT-fat.jar
```

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
PK: user_id, meal_type, scheduled_date 
FK: recipe_id → recipe(recipe_id), user_id → user(user_id)
```

## Project Structure

```text
📦 le-festin
 ┣ 📂 sql
 ┃ ┣ 📜 le_festin_schema.sql         # Database table definitions
 ┃ ┣ 📜 le_festin_seed.sql           # Initial data for recipes/ingredients
 ┣ 📂 src
 ┃ ┗ 📂 main
 ┃   ┣ 📂 java
 ┃   ┃ ┗ 📂 com
 ┃   ┃   ┗ 📂 lefestin
 ┃   ┃     ┣ 📂 config               # DB Connection & Setup Utilities
 ┃   ┃     ┣ 📂 dao                  # Data Access Objects (Interfaces & Impl)
 ┃   ┃     ┣ 📂 helper               # Utility helpers
 ┃   ┃     ┣ 📂 model                # Entity POJOs (User, Recipe, etc.)
 ┃   ┃     ┣ 📂 service              # Business Logic (Auth, Matching, CSV)
 ┃   ┃     ┣ 📂 ui                   # Swing GUI (Frames, Panels, Dialogs)
 ┃   ┃     ┗ 📜 Main.java            # App Entry Point
 ┃   ┗ 📂 resources
 ┃     ┗ 📜 config.properties        # Active credentials (gitignored)
 ┣ 📂 target                         # Compiled classes and build artifacts
 ┣ 📜 .gitignore                     # Excludes config.properties and target/
 ┣ 📜 README.md                      # Project documentation
 ┣ 📜 config.properties.example      # Template for environment setup
 ┣ 📜 le-festin-1.0-SNAPSHOT-fat.jar # Executable fat JAR
 ┗ 📜 pom.xml                        # Maven configuration and dependencies
```

## Contributors
* Angela Almazan 
* Carl Rodriguez 
* Elizah Sumbeling 

