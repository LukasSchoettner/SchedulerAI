import { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import styles from './LocationPicker.module.css';

/**
 * Props:
 *  - value: {
 *      addressText?: string,
 *      latitude?: number,
 *      longitude?: number
 *    }
 *  - onChange: (newValue) => void
 */
function ClickHandler({ onClick }) {
    useMapEvents({
        click(e) {
            onClick(e.latlng);
        },
    });
    return null;
}

export default function LocationPicker({ value, onChange }) {
    const [addressText, setAddressText] = useState(value?.addressText || '');
    const [lat, setLat] = useState(value?.latitude ?? 48.99);  // default: near Regensburg
    const [lng, setLng] = useState(value?.longitude ?? 12.09);
    const [hasMarker, setHasMarker] = useState(
        value?.latitude != null && value?.longitude != null
    );

    // keep local state in sync if parent updates value from outside
    useEffect(() => {
        if (value?.addressText !== undefined) {
            setAddressText(value.addressText);
        }
        if (value?.latitude != null && value?.longitude != null) {
            setLat(value.latitude);
            setLng(value.longitude);
            setHasMarker(true);
        }
    }, [value]);

    const emitChange = (next) => {
        onChange?.({
            addressText,
            latitude: lat,
            longitude: lng,
            ...next,
        });
    };

    const handleMapClick = (latlng) => {
        setLat(latlng.lat);
        setLng(latlng.lng);
        setHasMarker(true);
        emitChange({ latitude: latlng.lat, longitude: latlng.lng });
    };

    const handleLatChange = (v) => {
        const num = v === '' ? null : Number(v);
        setLat(num ?? 0);
        setHasMarker(num != null && lng != null);
        emitChange({ latitude: num });
    };

    const handleLngChange = (v) => {
        const num = v === '' ? null : Number(v);
        setLng(num ?? 0);
        setHasMarker(lat != null && num != null);
        emitChange({ longitude: num });
    };

    const handleAddressChange = (v) => {
        setAddressText(v);
        emitChange({ addressText: v });
    };

    return (
        <div className={styles.wrapper}>
            <div className={styles.fields}>
                <label>
                    Address label / description
                    <input
                        type="text"
                        value={addressText}
                        onChange={(e) => handleAddressChange(e.target.value)}
                        placeholder="e.g. Gym, Grandma's house, Coworking space"
                    />
                </label>

                <div className={styles.coordRow}>
                    <label>
                        Latitude
                        <input
                            type="number"
                            step="0.000001"
                            value={lat ?? ''}
                            onChange={(e) => handleLatChange(e.target.value)}
                        />
                    </label>
                    <label>
                        Longitude
                        <input
                            type="number"
                            step="0.000001"
                            value={lng ?? ''}
                            onChange={(e) => handleLngChange(e.target.value)}
                        />
                    </label>
                </div>
                <small>
                    You can either enter coordinates manually or click on the map to set
                    them.
                </small>
            </div>

            <div className={styles.mapContainer}>
                <MapContainer
                    center={[lat || 48.99, lng || 12.09]}
                    zoom={13}
                    style={{ height: '300px', width: '100%' }}
                >
                    <TileLayer
                        attribution='&copy; OpenStreetMap contributors'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                    <ClickHandler onClick={handleMapClick} />
                    {hasMarker && <Marker position={[lat, lng]} />}
                </MapContainer>
            </div>
        </div>
    );
}
