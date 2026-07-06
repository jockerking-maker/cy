FROM node:18-alpine

WORKDIR /app

# Copy package files and install dependencies
COPY package.json ./
RUN npm install --production

# Copy source code
COPY . .

# Create directories for uploads and data
RUN mkdir -p uploads/images uploads/files data

EXPOSE 3001

CMD ["node", "src/app.js"]
