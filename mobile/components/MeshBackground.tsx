import React from 'react';
import { StyleSheet, View, Dimensions } from 'react-native';
import Svg, { Circle, Path, Defs, LinearGradient, Stop } from 'react-native-svg';

const { width, height } = Dimensions.get('window');

/**
 * Animated Mesh Background reflecting "The Mesh" design direction.
 * Visualizes the anonymous contact graph as a subtle, technical layer.
 */
export const MeshBackground = () => {
  return (
    <View style={StyleSheet.absoluteFill}>
      <Svg height="100%" width="100%" viewBox={`0 0 ${width} ${height}`}>
        <Defs>
          <LinearGradient id="grad" x1="0" y1="0" x2="1" y2="1">
            <Stop offset="0" stopColor="#0891B2" stopOpacity="0.1" />
            <Stop offset="1" stopColor="#09090b" stopOpacity="0.1" />
          </LinearGradient>
        </Defs>
        
        {/* Subtle Grid Points */}
        {[...Array(20)].map((_, i) => (
          <Circle
            key={i}
            cx={Math.random() * width}
            cy={Math.random() * height}
            r="1"
            fill="#0891B2"
            opacity={0.3}
          />
        ))}

        {/* Connection Lines (Static for now) */}
        <Path
          d={`M ${width * 0.2} ${height * 0.3} L ${width * 0.8} ${height * 0.4} L ${width * 0.5} ${height * 0.7} Z`}
          stroke="#0891B2"
          strokeWidth="0.5"
          fill="none"
          opacity={0.1}
        />
      </Svg>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#09090b',
  },
});
