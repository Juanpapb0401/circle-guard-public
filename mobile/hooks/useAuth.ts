import { useState, useEffect } from 'react';
import * as SecureStore from 'expo-secure-store';

/**
 * Hook to manage CircleGuard Identity Handshake.
 * Implements Story 1.1 & 2.1: Anonymous ID persistence.
 */
export const useAuth = () => {
  const [anonymousId, setAnonymousId] = useState<string | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadIdentity();
  }, []);

  const loadIdentity = async () => {
    try {
      const id = await SecureStore.getItemAsync('circleguard_anon_id');
      const storedToken = await SecureStore.getItemAsync('circleguard_token');
      setAnonymousId(id);
      setToken(storedToken);
    } catch (e) {
      console.error('Failed to load identity', e);
    } finally {
      setIsLoading(false);
    }
  };

  const enroll = async (id: string, newToken: string) => {
    await SecureStore.setItemAsync('circleguard_anon_id', id);
    await SecureStore.setItemAsync('circleguard_token', newToken);
    setAnonymousId(id);
    setToken(newToken);
  };

  const logout = async () => {
    await SecureStore.deleteItemAsync('circleguard_anon_id');
    await SecureStore.deleteItemAsync('circleguard_token');
    setAnonymousId(null);
    setToken(null);
  };

  return { anonymousId, token, isLoading, enroll, logout };
};
