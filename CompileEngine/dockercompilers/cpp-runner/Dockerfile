# Use Ubuntu as a stable base for GCC
FROM ubuntu:22.04

# Prevent interactive prompts
ENV DEBIAN_FRONTEND=noninteractive

# Install g++ and gcc
RUN apt-get update && apt-get install -y \
    g++ \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# No CMD needed; Java ExecutionConsumer sends 'g++' and './main' commands