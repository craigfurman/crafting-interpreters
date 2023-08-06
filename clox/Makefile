# Credit to https://github.com/munificent/craftinginterpreters/blob/master/util/c.make,
# This is closely based on that.

NAME := clox
CFLAGS := -std=c99 -Wall -Wextra -Werror -Wno-unused-parameter -Wno-unused-function

ifeq ($(MODE),debug)
	CFLAGS += -O0 -DDEBUG -g
	BUILD_DIR := build/debug
else
	CFLAGS += -O3 -flto
	BUILD_DIR := build/release
endif

HEADERS := $(wildcard *.h)
SOURCES := $(wildcard *.c)
OBJECTS := $(addprefix $(BUILD_DIR)/$(NAME)/, $(notdir $(SOURCES:.c=.o)))

.PHONY: default
build/$(NAME): $(OBJECTS)
	@ printf "%8s %-40s %s\n" $(CC) $@ "$(CFLAGS)"
	@ mkdir -p build
	@ $(CC) $(CFLAGS) $^ -o $@

# Compile object files.
$(BUILD_DIR)/$(NAME)/%.o: %.c $(HEADERS)
	@ printf "%8s %-40s %s\n" $(CC) $< "$(CFLAGS)"
	@ mkdir -p $(BUILD_DIR)/$(NAME)
	@ $(CC) -c $(C_LANG) $(CFLAGS) -o $@ $<

.PHONY: clean
clean:
	rm -rf build
