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
* **Make**: For compiling and packaging (`make --version` to verify).
* **MySQL Server 8.4+**: To host the application database.

## Getting Started

On first launch, Le Festin opens a **Setup Wizard** that guides you through entering your MySQL credentials, creating the schema, and loading seed data — no manual configuration needed.

### Option 1: Local Development (Build from Source)

Use this option if you want to run the app directly from the code.

1. **Clone the repository**
```bash
git clone https://github.com/ausdotsn50/le-festin.git
cd le-festin/
```

2. **Build**
```bash
make
```
This compiles all sources, bundles the lib JARs, and produces `le-festin.jar`.

3. **Launch the app**
```bash
make run
```
The Setup Wizard will appear on first run — enter your MySQL connection details and follow the prompts to initialize the database.

## Option 2: Running the Executable

### 2a — JAR (cross-platform, requires Java 17+)

1. **Download the JAR**
   - Navigate to the [GitHub Repository](https://github.com/ausdotsn50/le-festin)
   - Open the `installers/` folder and download `le-festin.jar`

2. **Run**
   ```bash
   java -jar le-festin.jar
   ```
   The Setup Wizard will appear on first run — enter your MySQL connection details and follow the prompts to initialize the database.

### 2b — Windows Installer (via Inno Setup)

1. **Download the installer**
   - Navigate to the [GitHub Repository](https://github.com/ausdotsn50/le-festin)
   - Open the `installers/output/` folder and download `le-festin-1.0-setup.exe`

2. **Run the installer**
   - Double-click `le-festin-1.0-setup.exe` and follow the wizard
   - Choose an install directory (default: `C:\Program Files\Le Festin`)
   - Optionally create a desktop shortcut

3. **Launch**
   - Open **Le Festin** from the Start Menu or desktop shortcut
   - The Setup Wizard will appear on first run — enter your MySQL connection details and follow the prompts to initialize the database.

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
 ┣ 📂 lib                            # Local dependency JARs (no download needed)
 ┃ ┣ 📜 commons-csv-1.10.0.jar
 ┃ ┣ 📜 jbcrypt-0.4.jar
 ┃ ┗ 📜 mysql-connector-j-9.6.0.jar
 ┣ 📂 resources
 ┃ ┗ 📜 config.properties            # Active credentials (gitignored)
 ┣ 📂 sql
 ┃ ┣ 📜 le_festin_schema.sql         # Database table definitions
 ┃ ┗ 📜 le_festin_seed.sql           # Initial data for recipes/ingredients
 ┣ 📂 src
 ┃ ┣ 📂 config                       # DB Connection & Setup Utilities
 ┃ ┣ 📂 dao                          # Data Access Objects (Interfaces & Impl)
 ┃ ┣ 📂 helper                       # Utility helpers
 ┃ ┣ 📂 model                        # Entity POJOs (User, Recipe, etc.)
 ┃ ┣ 📂 service                      # Business Logic (Auth, Matching, CSV)
 ┃ ┣ 📂 ui                           # Swing GUI (Frames, Panels, Dialogs)
 ┃ ┗ 📜 Main.java                    # App Entry Point
 ┣ 📂 installers                      # Installer scripts for distribution
 ┃ ┣ 📜 le-festin.iss               # Inno Setup script (Windows installer)
 ┃ ┣ 📜 LeFestin.bat                # Windows launcher (bundled by installer)
 ┃ ┗ 📜 le-festin.jar               # Fat JAR used as installer source
 ┣ 📜 .gitignore                     # Excludes config.properties and out/
 ┣ 📜 README.md                      # Project documentation
 ┣ 📜 config.properties.example      # Template for environment setup
 ┣ 📜 manifest.txt                   # JAR manifest (defines Main-Class)
 ┣ 📜 Makefile                       # Build targets: compile, jar, run, clean
 ┗ 📜 le-festin.jar                  # Executable fat JAR (generated by make)
```

## Building the Installer (Inno Setup)

> **Requires Windows.** On macOS/Linux, run these steps inside a Windows VM or via [Wine](https://www.winehq.org).

### Prerequisites

| Tool | Download |
|------|----------|
| Inno Setup 6 | [jrsoftware.org/isdl.php](https://jrsoftware.org/isdl.php) |
| Java 17+ (on the build machine) | [adoptium.net](https://adoptium.net) |

### Build the installer

1. Clone or copy the full repository to a Windows machine
2. Build the fat JAR on that machine:
   ```bash
   cd le_festin
   make
   copy le-festin.jar ..\installers\le-festin.jar
   ```
3. Open **Inno Setup Compiler** (`Compil32.exe`)
4. Go to **File → Open** and select `installers/le-festin.iss`
5. Press **Ctrl+F9** (or **Build → Compile**) to compile
6. Output: `installers/output/le-festin-1.0-setup.exe`

### Test the installer

1. Run `le-festin-1.0-setup.exe`
2. Step through the wizard — accept the default install path or choose a custom one
3. Check **Create a desktop shortcut** if desired, then click **Install**
4. When installation finishes, click **Launch Le Festin** (or open it from the Start Menu)
5. The in-app Setup Wizard should appear — enter your MySQL credentials to verify the full flow
6. To test uninstall: **Settings → Apps → Le Festin → Uninstall**

### What the installer bundles

| Item | Installed to |
|------|-------------|
| `le-festin.jar` | `<install dir>\` |
| `LeFestin.bat` (launcher) | `<install dir>\` |

SQL files and `config.properties` are handled entirely at runtime by the in-app Setup Wizard — nothing extra needs to be installed.

### InstallForge (alternative GUI tool)

1. Download **InstallForge** from [installforge.net](https://installforge.net)
2. Create a new project — name: `Le Festin`, version: `1.0`
3. Under **Files**, add:
   - `installers/le-festin.jar` → install to `<InstallPath>\`
   - `le_festin/sql/` → install to `<InstallPath>\sql\`
   - `installers/LeFestin.bat` → install to `<InstallPath>\`
4. Set default install path to `%ProgramFiles%\Le Festin`
5. Add a shortcut pointing to `LeFestin.bat` and click **Build**

## Contributors
* Angela Almazan 
* Carl Rodriguez 
* Elizah Sumbeling
