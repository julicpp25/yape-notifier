import React from 'react';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import { WifiOff } from 'lucide-react';

export const OfflineIndicator: React.FC = () => {
  const isOnline = useOnlineStatus();

  if (isOnline) return null;

  return (
    <div className="fixed bottom-4 left-4 z-50 flex items-center gap-2 rounded-xl bg-amber-500/95 backdrop-blur-md px-4 py-2 text-xs font-semibold text-white shadow-xl ring-1 ring-amber-400/30 animate-bounce">
      <WifiOff className="w-4 h-4" />
      <span>Modo sin conexión — La app sigue funcionando con la caché PWA.</span>
    </div>
  );
};
