# ---- Étape 1 : Build de l'application Angular ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install

COPY . .
RUN npm run build
# --- pour optimiser limage builder
# ---- Étape 2 : Servir avec Nginx ----
FROM nginx:alpine
COPY --from=build /app/dist/gestion-projet/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
