SRC_DIR=src/main/java
OUT_DIR=out
PKG=terminalbuffer
MAIN=$(PKG).Main

JAVA_FILES=$(SRC_DIR)/$(PKG)/*.java

all: compile

compile:
	mkdir -p $(OUT_DIR)
	javac -d $(OUT_DIR) $(JAVA_FILES)

run: compile
	java -cp $(OUT_DIR) $(MAIN)

clean:
	rm -rf $(OUT_DIR)