import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';
import { AgentApp } from './app';

const root = document.getElementById('root');
if (!root) throw new Error('Missing #root');
createRoot(root).render(
  <StrictMode>
    <AgentApp />
  </StrictMode>,
);
