using System;
using System.IO.MemoryMappedFiles;
using System.Runtime.InteropServices;

namespace VRWCompanion.GamePlugins.AssettoCorsa
{
    /// <summary>
    /// Reads Assetto Corsa's "Physics" shared-memory block (AC exposes telemetry via named
    /// memory-mapped files: acpmf_physics, acpmf_graphics, acpmf_static). Converts into the
    /// companion's generic TelemetryFrame for the relay.
    ///
    /// UNVERIFIED against a real game session - no AC install was available during development.
    /// See README.md in this folder for which fields are solid vs. approximate vs. wrong-until-fixed.
    /// </summary>
    public class AssettoCorsaReader : IDisposable
    {
        private const string PhysicsMapName = "Local\\acpmf_physics";
        private MemoryMappedFile? _physicsFile;
        private MemoryMappedViewAccessor? _physicsAccessor;

        public bool Connect()
        {
            try
            {
                _physicsFile = MemoryMappedFile.OpenExisting(PhysicsMapName);
                _physicsAccessor = _physicsFile.CreateViewAccessor();
                return true;
            }
            catch (FileNotFoundException)
            {
                // AC isn't running / shared memory not yet initialized.
                return false;
            }
        }

        public TelemetryFrame? ReadFrame()
        {
            if (_physicsAccessor == null) return null;

            try
            {
                var physics = ReadStruct<SPageFilePhysics>(_physicsAccessor);

                return new TelemetryFrame
                {
                    SpeedKmh = physics.SpeedKmh,
                    Rpm = physics.Rpms,
                    // AC's shared memory doesn't expose max RPM directly in the physics page;
                    // it lives in the static page which isn't wired up yet. Hardcoded for now -
                    // WRONG for any car that doesn't redline near 8000 RPM.
                    MaxRpm = 8000f,
                    Gear = physics.Gear - 1, // AC uses 0=reverse, 1=neutral, 2=first...
                    // AC reports fuel in liters already, NOT a percentage - do not treat this as 0-100%.
                    FuelLiters = physics.Fuel,
                    LapTimeSeconds = 0f, // not read from this page; would need acpmf_graphics
                    TireTemps = new float[]
                    {
                        physics.TyreCoreTemperature0,
                        physics.TyreCoreTemperature1,
                        physics.TyreCoreTemperature2,
                        physics.TyreCoreTemperature3
                    }
                    // NOTE: clutch input is present in SPageFilePhysics (physics.Clutch) but is
                    // NOT currently read/forwarded anywhere in this plugin - flagged as a known gap.
                };
            }
            catch
            {
                return null;
            }
        }

        private static T ReadStruct<T>(MemoryMappedViewAccessor accessor) where T : struct
        {
            accessor.Read(0, out T result);
            return result;
        }

        public void Dispose()
        {
            _physicsAccessor?.Dispose();
            _physicsFile?.Dispose();
        }

        /// <summary>
        /// Partial mapping of AC's SPageFilePhysics struct - only the fields this plugin actually
        /// uses. Field offsets/layout follow AC's official shared memory documentation; since this
        /// has not been validated against a live AC session, offsets should be double-checked
        /// against the real SDK header before relying on this in production.
        /// </summary>
        [StructLayout(LayoutKind.Sequential, Pack = 4)]
        private struct SPageFilePhysics
        {
            public int PacketId;
            public float Gas;
            public float Brake;
            public float Fuel;
            public int Gear;
            public int Rpms;
            public float SteerAngle;
            public float SpeedKmh;
            // ... many fields omitted (velocity, accG, wheel slip, etc. - not used here) ...
            public float Clutch;
            public float TyreCoreTemperature0;
            public float TyreCoreTemperature1;
            public float TyreCoreTemperature2;
            public float TyreCoreTemperature3;
        }
    }
}
