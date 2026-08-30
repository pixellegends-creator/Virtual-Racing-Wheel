using System;
using System.Windows;
using Application = System.Windows.Application;

namespace WindowsCompanion
{
    public class App
    {
        [STAThread]
        public static void Main(string[] args)
        {
            bool headless = Array.IndexOf(args, "--headless") >= 0;

            if (headless)
            {
                HeadlessRunner.Run(args);
                return;
            }

            bool simulate = Array.IndexOf(args, "--simulate") >= 0;
            bool simulateTelemetry = Array.IndexOf(args, "--simulate-telemetry") >= 0;

            var app = new Application();
            var window = new MainWindow(simulate, simulateTelemetry);
            app.Run(window);
        }
    }
}
