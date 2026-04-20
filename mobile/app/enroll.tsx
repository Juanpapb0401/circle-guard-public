import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { MeshBackground } from '@/components/MeshBackground';
import { Shield, Smartphone } from 'lucide-react-native';
import { useAuth } from '@/hooks/useAuth';

/**
 * Frictionless Enrollment Landing Page.
 * Implements Story 1.1 & 2.1: Campus Identity Handshake.
 */
export default function EnrollmentScreen() {
  const { enroll } = useAuth();

  const handleEnroll = async () => {
    // Generate a secure anonymous ID (in real app, this comes from auth-service)
    const newId = Math.random().toString(36).substring(7);
    await enroll(newId);
    // Navigate home
  };

  return (
    <View style={styles.container}>
      <MeshBackground />
      
      <View style={styles.content}>
        <View style={styles.iconContainer}>
          <Shield size={48} color="#0891B2" />
        </View>

        <Text style={styles.title}>Secure Your Campus Identity</Text>
        <Text style={styles.subtitle}>
          CircleGuard uses anonymous vaults to protect your data. 
          One tap to begin your secure campus experience.
        </Text>

        <TouchableOpacity 
          style={styles.button} 
          activeOpacity={0.8}
          onPress={handleEnroll}
        >
          <Text style={styles.buttonText}>Start Frictionless Enroll</Text>
        </TouchableOpacity>

        <View style={styles.footer}>
          <Smartphone size={16} color="#52525b" />
          <Text style={styles.footerText}>Hardware-Attested Security Active</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#09090b',
    padding: 24,
    justifyContent: 'center',
  },
  content: {
    alignItems: 'center',
  },
  iconContainer: {
    marginBottom: 32,
    padding: 24,
    backgroundColor: 'rgba(8, 145, 178, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(8, 145, 178, 0.2)',
    borderRadius: 40,
  },
  title: {
    color: '#f4f4f5',
    fontSize: 28,
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 16,
    fontFamily: 'Outfit', // Assuming global font config
  },
  subtitle: {
    color: '#a1a1aa',
    fontSize: 16,
    lineHeight: 24,
    textAlign: 'center',
    marginBottom: 48,
    paddingHorizontal: 12,
  },
  button: {
    width: '100%',
    backgroundColor: '#0891B2',
    paddingVertical: 18,
    borderRadius: 16,
    alignItems: 'center',
    shadowColor: '#0891B2',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 10,
    elevation: 8,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  footer: {
    marginTop: 48,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  footerText: {
    color: '#52525b',
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
});
