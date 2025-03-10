#!/bin/bash

# Check if at least a directory, number of days, and one pattern are provided
if [ $# -lt 3 ]; then
    echo "Usage: $0 <parent_directory> <days_old> <pattern1> [<pattern2> ...]"
    exit 1
fi

PARENT_DIR="$1"
DAYS_OLD="$2"
shift 2  # Remove the first two arguments (directory & days), so $@ contains only patterns

# Check if the directory exists
if [ ! -d "$PARENT_DIR" ]; then
    echo "Error: Directory '$PARENT_DIR' does not exist."
    exit 1
fi

# Check if the days argument is a valid number
if ! [[ "$DAYS_OLD" =~ ^[0-9]+$ ]]; then
    echo "Error: Days argument must be a positive number."
    exit 1
fi

# Loop through all patterns provided as arguments
for pattern in "$@"; do
    echo "Deleting files matching pattern: $pattern in $PARENT_DIR and its subdirectories (older than $DAYS_OLD days)..."
    find "$PARENT_DIR" -type f -name "$pattern" -mtime +"$DAYS_OLD" -exec rm -f {} \;
done

echo "Cleanup completed!"
