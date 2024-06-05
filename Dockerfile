FROM node:12-slim

# Create app directory
WORKDIR /usr/src/app

# Install app dependencies
COPY package*.json ./
RUN npm install elastic-apm-node --save

# Bundle app source
COPY . .

EXPOSE 9999
CMD [ "node", "app.js" ]
