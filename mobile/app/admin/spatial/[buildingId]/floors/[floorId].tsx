import React, { useEffect, useState, useRef } from 'react';
import { View, Text, StyleSheet, Image, Pressable, ScrollView, TextInput, TouchableOpacity, ActivityIndicator, Alert, PanResponder, Dimensions } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { useSpatial, AccessPoint, Floor } from '@/hooks/useSpatial';

const MAP_WIDTH = 800;
const MAP_HEIGHT = 600;

export default function MapEditorScreen() {
  const { buildingId, floorId: initialFloorId } = useLocalSearchParams<{ buildingId: string, floorId: string }>();
  const { getFloors, getAccessPoints, saveAccessPoint, deleteAccessPoint } = useSpatial();

  const [floors, setFloors] = useState<Floor[]>([]);
  const [selectedFloorId, setSelectedFloorId] = useState<string>(initialFloorId === 'default' ? '' : initialFloorId);
  const [aps, setAps] = useState<AccessPoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedAp, setSelectedAp] = useState<AccessPoint | null>(null);
  
  // Sidebar state for editing
  const [editAp, setEditAp] = useState<AccessPoint | null>(null);

  useEffect(() => {
    const fetchFloors = async () => {
      const data = await getFloors(buildingId);
      setFloors(data);
      if (data.length > 0 && !selectedFloorId) {
        setSelectedFloorId(data[0].id);
      }
    };
    fetchFloors();
  }, [buildingId]);

  useEffect(() => {
    if (selectedFloorId) {
      fetchAps();
    }
  }, [selectedFloorId]);

  const fetchAps = async () => {
    setLoading(true);
    const data = await getAccessPoints(selectedFloorId);
    setAps(data);
    setLoading(false);
  };

  const handleMapClick = (event: any) => {
    // For Web, we can get locationX/Y from the native event
    const { locationX, locationY } = event.nativeEvent;
    
    // Create a new AP at this location
    const newAp: AccessPoint = {
      macAddress: '00:00:00:00:00:00',
      name: `AP-${aps.length + 1}`,
      coordinateX: locationX,
      coordinateY: locationY,
      floorId: selectedFloorId
    };
    
    setEditAp(newAp);
    setSelectedAp(null);
  };

  const handleSaveAp = async () => {
    if (!editAp) return;
    await saveAccessPoint(selectedFloorId, editAp);
    setEditAp(null);
    fetchAps();
  };

  const handleDeleteAp = async (id: string) => {
    await deleteAccessPoint(id);
    setSelectedAp(null);
    setEditAp(null);
    fetchAps();
  };

  const handleDrag = (apId: string, x: number, y: number) => {
    setAps(prev => prev.map(ap => 
      ap.id === apId ? { ...ap, coordinateX: x, coordinateY: y } : ap
    ));
  };

  const finalizeDrag = async (ap: AccessPoint) => {
    await saveAccessPoint(selectedFloorId, ap);
  };

  return (
    <View style={styles.container}>
      <View style={styles.sidebar}>
        <Text style={styles.sidebarTitle}>Floors</Text>
        <ScrollView style={styles.floorList}>
          {floors.map(f => (
            <TouchableOpacity 
              key={f.id} 
              onPress={() => setSelectedFloorId(f.id)}
              style={[styles.floorItem, selectedFloorId === f.id && styles.floorItemSelected]}
            >
              <Text style={[styles.floorText, selectedFloorId === f.id && styles.floorTextSelected]}>
                Floor {f.floorNumber} - {f.name}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        <View style={styles.divider} />

        {editAp || selectedAp ? (
          <View style={styles.editSection}>
            <Text style={styles.sidebarTitle}>{editAp?.id || selectedAp?.id ? 'Edit AP' : 'New AP'}</Text>
            <View style={styles.formItem}>
              <Text style={styles.label}>Name</Text>
              <TextInput 
                style={styles.input} 
                value={editAp?.name || selectedAp?.name} 
                onChangeText={(t) => editAp ? setEditAp({...editAp, name: t}) : setSelectedAp({...selectedAp!, name: t})}
              />
            </View>
            <View style={styles.formItem}>
              <Text style={styles.label}>MAC Address</Text>
              <TextInput 
                style={styles.input} 
                value={editAp?.macAddress || selectedAp?.macAddress} 
                onChangeText={(t) => editAp ? setEditAp({...editAp, macAddress: t}) : setSelectedAp({...selectedAp!, macAddress: t})}
              />
            </View>
            <View style={styles.coords}>
              <Text style={styles.coordLabel}>X: {(editAp?.coordinateX || selectedAp?.coordinateX || 0).toFixed(0)}</Text>
              <Text style={styles.coordLabel}>Y: {(editAp?.coordinateY || selectedAp?.coordinateY || 0).toFixed(0)}</Text>
            </View>
            <TouchableOpacity style={styles.saveButton} onPress={handleSaveAp}>
              <Text style={styles.saveButtonText}>Save Access Point</Text>
            </TouchableOpacity>
            {(editAp?.id || selectedAp?.id) && (
              <TouchableOpacity style={styles.deleteButton} onPress={() => handleDeleteAp(editAp?.id || selectedAp!.id!)}>
                <Text style={styles.deleteButtonText}>Delete</Text>
              </TouchableOpacity>
            )}
            <TouchableOpacity style={styles.cancelButton} onPress={() => { setEditAp(null); setSelectedAp(null); }}>
              <Text style={styles.cancelText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <View style={styles.hintSection}>
            <Text style={styles.hintText}>Click anywhere on the map to place a new Access Point.</Text>
            <Text style={styles.hintText}>Drag existing points to reposition them.</Text>
          </View>
        )}
      </View>

      <View style={styles.mapArea}>
        {loading ? (
          <ActivityIndicator size="large" color="#06b6d4" />
        ) : (
          <View style={styles.mapContainer}>
             <Pressable onPress={handleMapClick}>
              <Image 
                source={{ uri: 'https://images.unsplash.com/photo-1541888946425-d81bb19480c5?auto=format&fit=crop&q=80&w=1000' }}
                style={styles.floorPlan}
                resizeMode="contain"
              />
             </Pressable>
            {aps.map(ap => (
              <DraggableAP 
                key={ap.id || 'new'} 
                ap={ap} 
                isSelected={selectedAp?.id === ap.id}
                onSelect={() => { setSelectedAp(ap); setEditAp(null); }}
                onDrag={handleDrag}
                onDragEnd={finalizeDrag}
              />
            ))}
          </View>
        )}
      </View>
    </View>
  );
}

function DraggableAP({ ap, isSelected, onSelect, onDrag, onDragEnd }: { ap: AccessPoint, isSelected: boolean, onSelect: () => void, onDrag: (id: string, x: number, y: number) => void, onDragEnd: (ap: AccessPoint) => void }) {
  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onPanResponderMove: (_, gestureState) => {
        // Correctly calculate new position relative to map container
        // Note: gestureState.moveX/Y are screen coords, so we'd need container offset
        // Simplified for this demo: use delta
        onDrag(ap.id!, ap.coordinateX + gestureState.dx, ap.coordinateY + gestureState.dy);
      },
      onPanResponderRelease: () => {
        onDragEnd(ap);
      },
    })
  ).current;

  return (
    <View 
      {...panResponder.panHandlers}
      style={[
        styles.apMarker, 
        { top: ap.coordinateY - 12, left: ap.coordinateX - 12 },
        isSelected && styles.apMarkerSelected
      ]}
    >
      <Pressable onPress={onSelect} style={{ width: '100%', height: '100%' }} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000', flexDirection: 'row' },
  sidebar: { width: 300, backgroundColor: '#111', borderRightWidth: 1, borderRightColor: '#27272a', padding: 20 },
  sidebarTitle: { color: '#71717a', fontSize: 12, fontWeight: 'bold', textTransform: 'uppercase', marginBottom: 15 },
  floorList: { maxHeight: 200, marginBottom: 20 },
  floorItem: { padding: 12, borderRadius: 8, marginBottom: 8, backgroundColor: '#18181b' },
  floorItemSelected: { backgroundColor: '#0891b2' },
  floorText: { color: '#a1a1aa' },
  floorTextSelected: { color: '#fff', fontWeight: 'bold' },
  divider: { height: 1, backgroundColor: '#27272a', marginVertical: 20 },
  editSection: { flex: 1 },
  formItem: { marginBottom: 15 },
  label: { color: '#a1a1aa', fontSize: 12, marginBottom: 5 },
  input: { backgroundColor: '#09090b', color: '#fff', padding: 10, borderRadius: 6, borderWidth: 1, borderColor: '#27272a' },
  coords: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
  coordLabel: { color: '#06b6d4', fontFamily: 'monospace', fontSize: 14 },
  saveButton: { backgroundColor: '#0891b2', padding: 12, borderRadius: 8, alignItems: 'center' },
  saveButtonText: { color: '#fff', fontWeight: 'bold' },
  deleteButton: { backgroundColor: '#7f1d1d', padding: 12, borderRadius: 8, alignItems: 'center', marginTop: 10 },
  deleteButtonText: { color: '#fff' },
  cancelButton: { padding: 12, alignItems: 'center', marginTop: 5 },
  cancelText: { color: '#71717a' },
  hintSection: { padding: 20, backgroundColor: '#18181b', borderRadius: 12, borderStyle: 'dashed', borderWidth: 1, borderColor: '#27272a' },
  hintText: { color: '#71717a', fontSize: 13, marginBottom: 10, lineHeight: 18 },
  mapArea: { flex: 1, justifyContent: 'center', alignItems: 'center', background: '#09090b' },
  mapContainer: { width: MAP_WIDTH, height: MAP_HEIGHT, backgroundColor: '#000', borderRadius: 8, overflow: 'hidden', position: 'relative', borderWidth: 1, borderColor: '#27272a' },
  floorPlan: { width: '100%', height: '100%', opacity: 0.6 },
  apMarker: { position: 'absolute', width: 24, height: 24, backgroundColor: '#0891b2', borderRadius: 12, borderWidth: 2, borderColor: '#fff', shadowColor: '#06b6d4', shadowRadius: 10, shadowOpacity: 0.8 },
  apMarkerSelected: { backgroundColor: '#fff', borderColor: '#0891b2', transform: [{ scale: 1.2 }] },
});
