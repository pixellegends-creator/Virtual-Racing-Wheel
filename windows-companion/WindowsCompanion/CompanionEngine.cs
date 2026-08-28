using System;
using System.Collections.Generic;
using System.Collections.Concurrent;

namespace WindowsCompanion
{
    /// <summary>
    /// Shared engine that owns UDP receive, pairing/whitelisting, virtual controller output, and
    /// telemetry relay. Both the WPF GUI and the --headless console mode wrap this identically so
    /// behavior never diverges between the two front ends.
    /// </summary>
    public class CompanionEngine
    {
        public event Action<string>? ActivityLogged;
        public event Action<TrustedDevice>? PairingRequested;
        public event Action<ConnectionStats>? StatsUpdated;

        private readonly ConcurrentDictionary<string, TrustedDevice> _trustedDevices = new();
        private readonly Random _pinRandom = new();

        public IReadOnlyCollection<TrustedDevice> TrustedDevices => (IReadOnlyCollection<TrustedDevice>)_trustedDevices.Values;

        public IVirtualControllerOutput ControllerOutput { get; }
        public bool SimulateMode { get; }
        public bool SimulateTelemetryMode { get; }

        public CompanionEngine(IVirtualControllerOutput controllerOutput, bool simulate = false, bool simulateTelemetry = false)
        {
            ControllerOutput = controllerOutput;
            SimulateMode = simulate;
            SimulateTelemetryMode = simulateTelemetry;
        }

        public string GeneratePin()
        {
            return _pinRandom.Next(1000, 9999).ToString();
        }

        public TrustedDevice RequestPairing(string deviceId, string ipAddress)
        {
            var pin = GeneratePin();
            var device = new TrustedDevice
            {
                DeviceId = deviceId,
                IpAddress = ipAddress,
                Pin = pin,
                Trusted = false,
                LastSeenUtc = DateTime.UtcNow
            };
            _trustedDevices[deviceId] = device;
            PairingRequested?.Invoke(device);
            Log($"Pairing requested from {ipAddress} (device {deviceId}), PIN {pin}");
            return device;
        }

        public bool ConfirmPairing(string deviceId, string enteredPin)
        {
            if (!_trustedDevices.TryGetValue(deviceId, out var device)) return false;
            if (device.Pin != enteredPin) return false;

            device.Trusted = true;
            Log($"Device {deviceId} confirmed and trusted.");
            return true;
        }

        public bool IsTrusted(string deviceId)
        {
            return _trustedDevices.TryGetValue(deviceId, out var d) && d.Trusted;
        }

        public void TouchLastSeen(string deviceId)
        {
            if (_trustedDevices.TryGetValue(deviceId, out var d))
            {
                d.LastSeenUtc = DateTime.UtcNow;
            }
        }

        public bool RevokeDevice(string deviceId)
        {
            var removed = _trustedDevices.TryRemove(deviceId, out _);
            if (removed) Log($"Device {deviceId} revoked.");
            return removed;
        }

        public void ApplyControlFrame(ControlFrame frame)
        {
            ControllerOutput.Update(frame);
        }

        public void ReportStats(ConnectionStats stats)
        {
            StatsUpdated?.Invoke(stats);
        }

        public void Log(string message)
        {
            ActivityLogged?.Invoke($"[{DateTime.Now:HH:mm:ss}] {message}");
        }
    }

    public class TrustedDevice
    {
        public required string DeviceId { get; set; }
        public required string IpAddress { get; set; }
        public required string Pin { get; set; }
        public bool Trusted { get; set; }
        public DateTime LastSeenUtc { get; set; }
    }

    public struct ControlFrame
    {
        public float Steering;
        public float Throttle;
        public float Brake;
        public float Clutch;
        public float CameraX;
        public float CameraY;
        public long SequenceNumber;
    }

    public struct ConnectionStats
    {
        public double LatencyMs;
        public double PacketLossPercent;
        public int ConnectedDeviceCount;
    }

    public interface IVirtualControllerOutput
    {
        void Update(ControlFrame frame);
        void Reset();
    }
}
