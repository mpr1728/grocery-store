# Grocery Store UI (ReactJS)

This is the frontend for the Grocery Store web application. It includes customer and admin portals for managing and purchasing grocery items.

## Features

- Customer registration and login
- Product browsing
- Membership plan display
- Admin dashboard to add grocery items
- Token-based authentication using JWT

## Prerequisites

- Node.js (v16 or later)
- npm or yarn
- React Router
- Axios

## Getting Started

1. **Install dependencies**  
   Navigate into the UI project folder and run:
   ```bash
   npm install
   ```

2. **Run the development server**
   ```bash
   npm start
   ```

3. **Access the app**
   Open your browser and go to [http://localhost:3000](http://localhost:3000)

## Folder Structure

```
src/
├── App.js
├── index.js
├── components/
│   ├── Admin/           # Admin portal UI
│   ├── Customer/        # Customer-facing components
│   └── Shared/          # Reusable components like Navbar
├── services/            # Axios client and auth helpers
└── styles/              # Global styles
```

## API Integration

The app connects to a backend BFF (Spring Boot) running at `http://localhost:8081`. Update `src/services/api.js` if your BFF URL changes.

## End-to-End Test

1. Start your backend BFF and microservices.
2. Launch this React app using `npm start`.
3. Register as a customer or login as an admin.
4. Browse products and test admin functionality.

## Build for Production

```bash
npm run build
```

This creates a production-ready build in the `build/` folder.
