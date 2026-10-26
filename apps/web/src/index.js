import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';
const root = ReactDOM.createRoot(document.getElementById('root')); // 반드시 index.html의 <div id="root">와 연결
root.render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);
