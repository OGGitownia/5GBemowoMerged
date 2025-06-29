import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import AppRouter from './AppRouter';
import './index.css';
import {AppProvider} from "./services/AppContext.tsx";


ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
    <React.StrictMode>
        <BrowserRouter>
            <AppProvider>
                <AppRouter />
            </AppProvider>
        </BrowserRouter>
    </React.StrictMode>
);
