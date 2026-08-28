using System;
using System.Threading;

namespace WindowsCompanion
{
    /// <summary>
    /// --headless entry point: runs CompanionEngine with a console front end instead of the WPF
    /// GUI, using identical underlying behavior. --simulate feeds fake control frames through the
    /// virtual controller output for diagnosing driver/ViGEmBus/vJoy issues without a phone
    /// connected. --simulate-telemetry does the same for the telemetry relay/dashboard path.
    /// </summary>
    public static class HeadlessRunner
    {
        public static void Run(string[] args)
        {
            bool simulate = Array.IndexOf(args, "--simulate") >= 0;
            bool simulateTelemetry = Array.IndexOf(args, "--simulate-telemetry") >= 0;

            IVirtualControllerOutput output = simulate
                ? new NullControllerOutput(logToConsole: true)
                : new ViGEmVJoyOutput(new NoopVJoyClutchDevice());

            var engine = new CompanionEngine(output, simulate, simulateTelemetry);
            engine.ActivityLogged += msg => Console.WriteLine(msg);

            Console.WriteLine("VRW Companion — headless mode");
            Console.WriteLine("Commands: list, revoke <deviceId>, quit");

            if (simulate)
            {
                RunSimulation(engine);
            }

            if (simulateTelemetry)
            {
                RunTelemetrySimulation();
            }

            while (true)
            {
                var line = Console.ReadLine();
                if (line == null) continue;
                var parts = line.Split(' ', StringSplitOptions.RemoveEmptyEntries);
                if (parts.Length == 0) continue;

                switch (parts[0])
                {
                    case "list":
                        foreach (var d in engine.TrustedDevices)
                            Console.WriteLine($"{d.DeviceId} ({d.IpAddress}) trusted={d.Trusted}");
                        break;
                    case "revoke":
                        if (parts.Length > 1) engine.RevokeDevice(parts[1]);
                        break;
                    case "quit":
                        return;
                    default:
                        Console.WriteLine("Unknown command.");
                        break;
                }
            }
        }

        private static void RunSimulation(CompanionEngine engine)
        {
            Console.WriteLine("Simulating control frames (sine-wave steering, ramping throttle)...");
            var thread = new Thread(() =>
            {
                long seq = 0;
                double t = 0;
                while (true)
                {
                    var frame = new ControlFrame
                    {
                        Steering = (float)Math.Sin(t),
                        Throttle = (float)((Math.Sin(t / 2) + 1) / 2),
                        Brake = 0f,
                        Clutch = 0f,
                        CameraX = 0f,
                        CameraY = 0f,
                        SequenceNumber = seq++
                    };
                    engine.ApplyControlFrame(frame);
                    t += 0.1;
                    Thread.Sleep(50);
                }
            });
            thread.IsBackground = true;
            thread.Start();
        }

        private static void RunTelemetrySimulation()
        {
            Console.WriteLine("Simulating telemetry frames on relay port 45128...");
            var relay = new TelemetryRelay();
            var thread = new Thread(() =>
            {
                double t = 0;
                while (true)
                {
                    var frame = new TelemetryFrame
                    {
                        SpeedKmh = (float)(100 + 50 * Math.Sin(t)),
                        Rpm = (float)(4000 + 2000 * Math.Sin(t * 2)),
                        MaxRpm = 8000f,
                        Gear = 3,
                        FuelLiters = (float)(40 - t % 40),
                        LapTimeSeconds = (float)(t % 120),
                        TireTemps = new float[] { 85f, 86f, 83f, 84f }
                    };
                    relay.Broadcast(frame);
                    t += 0.1;
                    Thread.Sleep(100);
                }
            });
            thread.IsBackground = true;
            thread.Start();
        }
    }

    /// <summary>No-op controller output used when ViGEmBus/vJoy drivers aren't installed (e.g. CI, or --simulate without real output).</summary>
    public class NullControllerOutput : IVirtualControllerOutput
    {
        private readonly bool _logToConsole;
        public NullControllerOutput(bool logToConsole = false) { _logToConsole = logToConsole; }

        public void Update(ControlFrame frame)
        {
            if (_logToConsole)
            {
                Console.Write($"\rsteer={frame.Steering:F2} throttle={frame.Throttle:F2} brake={frame.Brake:F2} clutch={frame.Clutch:F2}   ");
            }
        }

        public void Reset() { }
    }

    public class NoopVJoyClutchDevice : IVJoyClutchDevice
    {
        public void Acquire() { }
        public void SetClutchAxis(float normalized01) { }
        public void Release() { }
    }
}
