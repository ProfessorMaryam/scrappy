#!/bin/bash

echo "=========================================="
echo "ScrapBH Marketplace Setup Verification"
echo "=========================================="
echo ""

# Check Java version
echo "1. Checking Java version..."
java -version 2>&1 | head -n 1
echo ""

# Check Maven version
echo "2. Checking Maven version..."
mvn -version | head -n 1
echo ""

# Verify project structure
echo "3. Verifying project structure..."
if [ -f "pom.xml" ]; then
    echo "✓ pom.xml found"
else
    echo "✗ pom.xml not found"
fi

if [ -f "src/main/resources/application.properties" ]; then
    echo "✓ application.properties found"
else
    echo "✗ application.properties not found"
fi

if [ -f "src/main/resources/db/schema.sql" ]; then
    echo "✓ schema.sql found"
else
    echo "✗ schema.sql not found"
fi

if [ -f ".env.example" ]; then
    echo "✓ .env.example found"
else
    echo "✗ .env.example not found"
fi
echo ""

# Count entity files
echo "4. Checking entity files..."
entity_count=$(find src/main/java/com/scrapbh/marketplace/entity -name "*.java" 2>/dev/null | wc -l)
echo "   Found $entity_count entity files (expected: 7)"
echo ""

# Count enum files
echo "5. Checking enum files..."
enum_count=$(find src/main/java/com/scrapbh/marketplace/enums -name "*.java" 2>/dev/null | wc -l)
echo "   Found $enum_count enum files (expected: 5)"
echo ""

# Compile check
echo "6. Attempting to compile project..."
mvn clean compile -q
if [ $? -eq 0 ]; then
    echo "✓ Project compiled successfully"
else
    echo "✗ Compilation failed - check Maven output above"
fi
echo ""

echo "=========================================="
echo "Setup verification complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Copy .env.example to .env and fill in your credentials"
echo "2. Set up your Supabase database using DATABASE_SETUP.md"
echo "3. Run 'mvn spring-boot:run' to start the application"
