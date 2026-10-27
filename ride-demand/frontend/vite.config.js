import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// /api calls go to the Spring Boot server, so no CORS headaches while developing
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
});
