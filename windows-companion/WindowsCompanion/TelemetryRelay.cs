using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;

namespace WindowsCompanion
{
    public struct TelemetryFrame
    {
        public float SpeedKmh;
        public float Rpm;
        public float MaxRpm;
        public int Gear;
        public float FuelLiters;
        public float LapTimeSeconds;
        public float[] TireTemps; // FL, FR, RL, RR
    }

    /// <summary>
    /// Receives telemetry from a game plugin on port 45127 and fans it out via UDP to every
    /// currently-trusted device's IP on port 45128. Originally single-target; Phase 8 multi-device
    /// support fixed the real bottleneck here (fan-out) rather than in pairing, which already
    /// supported multiple trusted devices.
    /// </summary>
    public class TelemetryRelay : IDisposable
    {
        private const int InPort = 45127;
        private const int OutPort = 45128;
        private readonly UdpClient _outSocket = new UdpClient();
        private readonly List<string> _targetIps = new();

        public void SetTargets(IEnumerable<string> ipAddresses)
        {
            _targetIps.Clear();
            _targetIps.AddRange(ipAddresses);
        }

        public void Broadcast(TelemetryFrame frame)
        {
            var bytes = Encode(frame);
            foreach (var ip in _targetIps)
            {
                try
                {
                    _outSocket.Send(bytes, bytes.Length, ip, OutPort);
                }
                catch
                {
                    // Best-effort UDP fan-out; one unreachable device shouldn't block the others.
                }
            }
        }

        public static byte[] Encode(TelemetryFrame frame)
        {
            using var stream = new System.IO.MemoryStream();
            using var writer = new System.IO.BinaryWriter(stream);
            writer.Write(frame.SpeedKmh);
            writer.Write(frame.Rpm);
            writer.Write(frame.MaxRpm);
            writer.Write(frame.Gear);
            writer.Write(frame.FuelLiters);
            writer.Write(frame.LapTimeSeconds);
            var temps = frame.TireTemps ?? new float[4];
            for (int i = 0; i < 4; i++)
            {
                writer.Write(i < temps.Length ? temps[i] : 0f);
            }
            return stream.ToArray();
        }

        public void Dispose()
        {
            _outSocket.Dispose();
        }
    }
}
