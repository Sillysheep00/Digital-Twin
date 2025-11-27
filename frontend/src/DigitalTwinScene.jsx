import React from 'react';
import "aframe";

const DigitalTwinScene = ({ data, onRoomSelect }) => {
  
  // Helper to get room data safely
  const getRoomData = (id) => {
    if (!data || !data.rooms) return null;
    return data.rooms.find(r => r.id === id);
  };

  const handleRoomClick = (roomId) => {
      console.log("Room Clicked:", roomId);
      if (onRoomSelect) {
          onRoomSelect(roomId);
      }
  };

  // Force models to be invisible on load
  const handleModelLoaded = (e) => {
      const model = e.detail.model;
      model.traverse((node) => {
          if (node.isMesh) {
              node.material.transparent = true;
              node.material.opacity = 0;
              node.material.needsUpdate = true;
          }
      });
  };

  // Configuration with ORIGINAL BLENDER COORDINATES
  const roomsConfig = [
    {
      id: "R1", // Meeting Room
      hv: { pos: "-3.377 -2.7709 0.53871" },
      s:  { pos: "-3.3521 -3.9721 2.6694" },
      e:  { pos: "-3.353 -2.5781 2.6551" }
    },
    {
      id: "R2", // Staff Lounge
      hv: { pos: "-5.9416 0.50125 0.54635" },
      s:  { pos: "-6.2632 -0.050213 2.4848" },
      e:  { pos: "-5.982 0.76898 2.3466" }
    },
    {
      id: "R3", // Main Office
      hv: { pos: "-0.50559 2.0946 1.9099" },
      s:  { pos: "-0.094914 -0.34172 2.5074" },
      e:  { pos: "0.062625 -0.6531 2.521" },
      isHorizontal: true
    },
    {
      id: "R4", // Boss Office
      hv: { pos: "3.293 -4.7866 0.72597" },
      s:  { pos: "2.8774 -2.3992 2.7007" },
      e:  { pos: "2.8537 -2.0142 2.7066" },
      isHorizontal: true
    },
    {
      id: "R5", // Washroom 1
      s:  { pos: "2.2654 -2.4155 2.6615" }
    },
    {
      id: "R6", // Washroom 2
      s:  { pos: "0.89854 -2.4183 2.6615" }
    },
    {
      id: "R7", // Focus Room
      hv: { pos: "-0.19664 -2.7709 0.53871" },
      s:  { pos: "-3.0701 -3.7636 2.6705" },
      e:  { pos: "-3.1005 -2.7441 2.6408" }
    }
  ];

  return (
    <div style={{ height: '600px', width: '100%' }}>
      <a-scene embedded background="color: #ECECEC">
        
        {/* LIGHTING */}
        <a-light type="ambient" color="#FFF" intensity="0.6"></a-light>
        <a-light type="directional" position="-1 10 5" intensity="0.8"></a-light>
        
        {/* CAMERA WITH CURSOR */}
        <a-entity camera look-controls wasd-controls position="0 5 10">
             <a-entity cursor="rayOrigin: mouse" raycaster="objects: .clickable"></a-entity>
        </a-entity>

        {/* 1. STATIC BLUEPRINT (Walls/Floor) */}
        <a-gltf-model
            id="blueprint-scan"
            src="/models/smartoffice.glb" 
            position="0 0 0"
        ></a-gltf-model>

        {/* 2. CLICKABLE ROOM MODELS (Aligned with Blueprint - NO ROTATION WRAPPER) */}
        {roomsConfig.map((room) => (
             <a-gltf-model
                key={`room-${room.id}`}
                src={`/models/${room.id}.glb`} 
                class="clickable"
                visible="false"
                onClick={() => handleRoomClick(room.id)}
                onMouseEnter={(e) => { 
                    e.target.setAttribute('visible', 'true');
                    e.target.object3D.traverse((node) => { 
                        if(node.isMesh) {
                            node.material.transparent = true;
                            node.material.opacity = 0.3; 
                            node.material.needsUpdate = true; 
                        } 
                    }); 
                }}
                onMouseLeave={(e) => { 
                    e.target.setAttribute('visible', 'false');
                }}
            ></a-gltf-model>
        ))}

        {/* 3. GENERATE INDICATORS (Rotated Wrapper to fix Blender Coordinates) */}
        <a-entity rotation="-90 0 0">
            {roomsConfig.map((room) => {
                const liveData = getRoomData(room.id);
                
                // Color Logic for Sensor (Temperature)
                let tempColor = "#00FFFF"; // Default Cyan
                if (liveData) {
                    if (liveData.temp > 24) tempColor = "red";       // Hot
                    else if (liveData.temp < 18) tempColor = "blue"; // Cold
                    else tempColor = "#00FF00";                      // Good (Green)
                }

                return (
                    <a-entity key={room.id} id={`group-${room.id}`}>

                        {/* HVAC INDICATOR (Swaps Width/Height based on isHorizontal prop) */}
                        {room.hv && (
                            <a-box 
                                position={room.hv.pos}
                                rotation={room.rot || "0 0 0"}
                                color="orange"
                                width={room.isHorizontal ? "0.81" : "0.116"} 
                                height={room.isHorizontal ? "0.116" : "0.81"} 
                                depth="0.579"
                                opacity={liveData?.hvac === "ON" ? 1 : 0.5}
                            ></a-box>
                        )}

                        {/* ENERGY METER (Teal Flat Panel - Vertical) */}
                        {room.e && (
                            <a-box 
                                position={room.e.pos}
                                rotation={room.rot || "0 0 0"}
                                color="#7FE7D7"
                                width="0.0468" height="0.328" depth="0.234"
                            ></a-box>
                        )}

                        {/* TEMPERATURE SENSOR (Sphere - Changes Color) */}
                        {room.s && (
                            <a-sphere 
                                position={room.s.pos}
                                color={tempColor}
                                radius="0.0665"
                            ></a-sphere>
                        )}

                    </a-entity>
                );
            })}
        </a-entity>

      </a-scene>
    </div>
  );
};

export default DigitalTwinScene;