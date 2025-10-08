#!/usr/bin/env bash

set -e

echo "======================================"
echo "Building ITSM Monorepo"
echo "======================================"
echo ""

# Build backend
echo "Building backend..."
cd backend
./gradlew build --no-daemon
cd ..
echo "✓ Backend build completed"
echo ""

# Build frontend
echo "Building frontend..."
cd frontend
npm install
npm run build
cd ..
echo "✓ Frontend build completed"
echo ""

echo "======================================"
echo "Build completed successfully!"
echo "======================================"
