import { useState, useEffect } from 'react';

/**
 * Hook to fetch and rotate short-lived Campus Entry QR tokens.
 * Implements Story 2.2: Rotating Token logic.
 */
export const useQrToken = (anonymousId: string | null) => {
  const [token, setToken] = useState<string | null>(null);
  const [timeLeft, setTimeLeft] = useState(60);

  useEffect(() => {
    if (!anonymousId) return;

    fetchToken();
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          fetchToken();
          return 60;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [anonymousId]);

  const fetchToken = async () => {
    try {
      // In a real app: const res = await api.get('/auth/qr/generate');
      // For now, generating a fake JWT-like string
      const fakeToken = `eyJhbm9uSWQiOiI${Math.random().toString(36).substring(7)}`;
      setToken(fakeToken);
      setTimeLeft(60);
    } catch (e) {
      console.error('QR Fetch Failed', e);
    }
  };

  return { token, timeLeft };
};
