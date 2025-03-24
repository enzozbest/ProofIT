FROM node:23

WORKDIR /app
COPY . .
RUN npm install
COPY vite.config.js .
CMD ["npm", "run", "dev"]
EXPOSE 5173
