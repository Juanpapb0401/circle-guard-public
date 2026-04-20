import { View, Text, StyleSheet } from 'react-native';
import { MeshBackground } from '@/components/MeshBackground';
import { QrCode } from 'lucide-react-native';
import { useAuth } from '@/hooks/useAuth';
import { useQrToken } from '@/hooks/useQrToken';
import { Link } from 'expo-router';

/**
 * Main Campus Entry Screen.
 * Implements Story 2.2: Secure Rotating QR Token display.
 */
export default function HomeScreen() {
  const { anonymousId } = useAuth();
  const { token, timeLeft } = useQrToken(anonymousId);

  return (
    <View style={styles.container}>
      <MeshBackground />
      
      <View style={styles.header}>
        <Text style={styles.statusLabel}>STATUS</Text>
        <Text style={styles.statusValue}>ACTIVE</Text>
      </View>

      <View style={styles.qrContainer}>
        <View style={styles.qrInner}>
          {/* In a real app, this would use react-native-qrcode-svg with 'token' */}
          <QrCode size={200} color="#f4f4f5" strokeWidth={1.5} />
        </View>
        <Text style={styles.expiresText}>
          EXPIRES IN {Math.floor(timeLeft / 60)}:{String(timeLeft % 60).padStart(2, '0')}
        </Text>
      </View>

      <View style={styles.footer}>
        <View style={styles.statBox}>
          <Text style={styles.statLabel}>FENCES</Text>
          <Text style={styles.statValue}>0 CLEAN</Text>
        </View>
        <View style={styles.statBox}>
          <Text style={styles.statLabel}>THE MESH</Text>
          <Text style={styles.statValue}>14 PEERS</Text>
        </View>
      </View>

      <Link href="/admin/spatial" asChild>
        <TouchableOpacity style={styles.adminButton}>
          <Text style={styles.adminButtonText}>ADMIN SPATIAL</Text>
        </TouchableOpacity>
      </Link>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#09090b',
    padding: 24,
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 80,
  },
  header: {
    alignItems: 'center',
  },
  statusLabel: {
    color: '#52525b',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 2,
    marginBottom: 4,
  },
  statusValue: {
    color: '#22c55e',
    fontSize: 48,
    fontWeight: '800',
    letterSpacing: -1,
  },
  qrContainer: {
    alignItems: 'center',
  },
  qrInner: {
    padding: 24,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: 32,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  expiresText: {
    marginTop: 24,
    color: '#0891B2',
    fontWeight: '700',
    fontSize: 14,
    letterSpacing: 1,
  },
  footer: {
    flexDirection: 'row',
    gap: 16,
    width: '100%',
  },
  statBox: {
    flex: 1,
    backgroundColor: 'rgba(24, 24, 27, 0.8)',
    padding: 16,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  statLabel: {
    color: '#52525b',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
    marginBottom: 4,
  },
  statValue: {
    color: '#f4f4f5',
    fontSize: 16,
    fontWeight: '700',
  },
  adminButton: {
    marginTop: 20,
    paddingVertical: 10,
    paddingHorizontal: 20,
    backgroundColor: 'rgba(255,255,255,0.05)',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#27272a',
  },
  adminButtonText: {
    color: '#71717a',
    fontSize: 12,
    fontWeight: 'bold',
    letterSpacing: 1,
  },
});
