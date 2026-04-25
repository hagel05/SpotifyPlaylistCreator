# Concert Playlist Builder - Frontend

React + Vite + TypeScript frontend for creating Spotify playlists from concert setlists.

## Setup

Install dependencies:
```bash
npm install
```

## Development

Start the development server:
```bash
npm run dev
```

The app will be available at `http://localhost:5173`

The Vite dev server is configured to proxy API calls to `http://localhost:8080` (backend).

## Build

Build for production:
```bash
npm run build
```

Preview production build:
```bash
npm run preview
```

## Tech Stack

- **React 18** - UI framework
- **Vite** - Build tool and dev server
- **TypeScript** - Type safety
- **React Router v6** - Client-side routing
- **Axios** - HTTP client
- **Tailwind CSS** - Utility-first styling
- **ESLint** - Code linting

## Project Structure

```
src/
├── pages/           # Page components (Search, Results, Playlist)
├── components/      # Reusable components
├── services/        # API client (Axios)
├── context/         # React Context (Auth)
├── App.tsx          # Main routing component
├── main.tsx         # React entry point
└── index.css        # Tailwind CSS imports
```

## Environment Variables

The backend API is proxied through Vite's dev server. For production, update the API base URL in `src/services/api.ts`.
