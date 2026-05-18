JAVAC    := javac
JAR_TOOL := jar
JAVA     := java

SRC_DIRS := src
OUT_DIR  := out
LIB_CP   := lib/mysql-connector-j-9.6.0.jar:lib/jbcrypt-0.4.jar:lib/commons-csv-1.10.0.jar
JAR_FILE := le-festin.jar

SOURCES := $(shell find $(SRC_DIRS) -name "*.java")

.PHONY: all compile jar run clean

all: jar

compile:
	mkdir -p $(OUT_DIR)
	$(JAVAC) -encoding UTF-8 --release 17 -cp $(LIB_CP) -d $(OUT_DIR) $(SOURCES)
	cp -r resources/. $(OUT_DIR)/

jar: compile
	cd $(OUT_DIR) && $(JAR_TOOL) xf ../lib/mysql-connector-j-9.6.0.jar
	cd $(OUT_DIR) && $(JAR_TOOL) xf ../lib/jbcrypt-0.4.jar
	cd $(OUT_DIR) && $(JAR_TOOL) xf ../lib/commons-csv-1.10.0.jar
	find $(OUT_DIR) -name "module-info.class" -delete
	rm -f $(OUT_DIR)/META-INF/MANIFEST.MF
	$(JAR_TOOL) cfm $(JAR_FILE) manifest.txt -C $(OUT_DIR) .

run: jar
	$(JAVA) -jar $(JAR_FILE)

clean:
	rm -rf $(OUT_DIR) $(JAR_FILE)