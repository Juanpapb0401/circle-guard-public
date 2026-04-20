import { useMemo } from 'react';
import { useAuth } from './useAuth';

/**
 * Hook to handle permission-based UI gating.
 * Implements Story 1.6.
 */
export const usePermission = () => {
    const { token } = useAuth();

    const permissions = useMemo(() => {
        if (!token) return [];
        try {
            // Basic JWT decoding (payload is the second part)
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(
                atob(base64)
                    .split('')
                    .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );
            const payload = JSON.parse(jsonPayload);
            return payload.permissions || [];
        } catch (e) {
            console.error('Failed to decode JWT permissions', e);
            return [];
        }
    }, [token]);

    const hasPermission = (permission: string): boolean => {
        return permissions.includes(permission);
    };

    return { hasPermission, permissions };
};
