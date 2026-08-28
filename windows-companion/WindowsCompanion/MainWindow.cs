using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Forms; // NotifyIcon (tray)
using Application = System.Windows.Application;

namespace WindowsCompanion
{
    /// <summary>
    /// Hand-built WPF UI (no XAML) so the whole GUI can be reviewed/edited as plain C#.
    /// Shows the current pairing PIN, live connection stats, the trusted-device list with
    /// per-device revoke buttons, a scrolling activity log, and hand-rolled latency/packet-loss
    /// trend graphs. Minimizes to a system tray icon; the app only fully exits via the tray menu.
    /// </summary>
    public class MainWindow : Window
    {
        private readonly CompanionEngine _engine;
        private readonly TextBlock _pinText = new() { FontSize = 28, FontWeight = FontWeights.Bold };
        private readonly TextBlock _statsText = new();
        private readonly StackPanel _deviceListPanel = new();
        private readonly ListBox _activityLog = new();
        private readonly TrendGraph _latencyGraph = new("Latency (ms)");
        private readonly TrendGraph _packetLossGraph = new("Packet Loss (%)");
        private NotifyIcon? _trayIcon;
        private bool _reallyExit;

        public MainWindow(bool simulate, bool simulateTelemetry)
        {
            Title = "Virtual Racing Wheel — Companion";
            Width = 720;
            Height = 560;

            IVirtualControllerOutput output = simulate
                ? new NullControllerOutput()
                : new ViGEmVJoyOutput(new NoopVJoyClutchDevice());

            _engine = new CompanionEngine(output, simulate, simulateTelemetry);
            _engine.ActivityLogged += msg => Dispatcher.Invoke(() => _activityLog.Items.Insert(0, msg));
            _engine.PairingRequested += device => Dispatcher.Invoke(() => _pinText.Text = $"PIN: {device.Pin}");
            _engine.StatsUpdated += stats => Dispatcher.Invoke(() =>
            {
                _statsText.Text = $"Latency: {stats.LatencyMs:F0}ms   Loss: {stats.PacketLossPercent:F1}%   Devices: {stats.ConnectedDeviceCount}";
                _latencyGraph.AddSample((float)stats.LatencyMs);
                _packetLossGraph.AddSample((float)stats.PacketLossPercent);
            });

            var root = new StackPanel { Margin = new Thickness(12) };
            root.Children.Add(new TextBlock { Text = "Pairing", FontWeight = FontWeights.Bold, FontSize = 16 });
            root.Children.Add(_pinText);
            root.Children.Add(new TextBlock { Text = "Live Stats", FontWeight = FontWeights.Bold, FontSize = 16, Margin = new Thickness(0, 12, 0, 0) });
            root.Children.Add(_statsText);
            root.Children.Add(_latencyGraph);
            root.Children.Add(_packetLossGraph);
            root.Children.Add(new TextBlock { Text = "Trusted Devices", FontWeight = FontWeights.Bold, FontSize = 16, Margin = new Thickness(0, 12, 0, 0) });
            root.Children.Add(_deviceListPanel);
            root.Children.Add(new TextBlock { Text = "Activity Log", FontWeight = FontWeights.Bold, FontSize = 16, Margin = new Thickness(0, 12, 0, 0) });
            _activityLog.Height = 140;
            root.Children.Add(_activityLog);

            Content = new ScrollViewer { Content = root };

            SetupTrayIcon();
            StateChanged += (_, _) =>
            {
                if (WindowState == WindowState.Minimized)
                {
                    Hide();
                    _trayIcon?.ShowBalloonTip(1000, "VRW Companion", "Minimized to tray", ToolTipIcon.Info);
                }
            };
            Closing += (_, e) =>
            {
                if (!_reallyExit)
                {
                    e.Cancel = true;
                    WindowState = WindowState.Minimized;
                }
            };

            RefreshDeviceList();
        }

        private void SetupTrayIcon()
        {
            _trayIcon = new NotifyIcon
            {
                Text = "VRW Companion",
                Visible = true,
                Icon = System.Drawing.SystemIcons.Application
            };

            var menu = new ContextMenuStrip();
            menu.Items.Add("Show", null, (_, _) => { Show(); WindowState = WindowState.Normal; });
            menu.Items.Add("Exit", null, (_, _) =>
            {
                _reallyExit = true;
                _trayIcon!.Visible = false;
                Application.Current.Shutdown();
            });
            _trayIcon.ContextMenuStrip = menu;
            _trayIcon.DoubleClick += (_, _) => { Show(); WindowState = WindowState.Normal; };
        }

        private void RefreshDeviceList()
        {
            _deviceListPanel.Children.Clear();
            foreach (var device in _engine.TrustedDevices)
            {
                var row = new DockPanel { Margin = new Thickness(0, 2, 0, 2) };
                row.Children.Add(new TextBlock { Text = $"{device.DeviceId} ({device.IpAddress}) — trusted={device.Trusted}", Width = 400 });
                var revokeButton = new Button { Content = "Revoke", Width = 80 };
                revokeButton.Click += (_, _) =>
                {
                    _engine.RevokeDevice(device.DeviceId);
                    RefreshDeviceList();
                };
                row.Children.Add(revokeButton);
                _deviceListPanel.Children.Add(row);
            }
        }
    }

    /// <summary>Minimal hand-rolled line-graph control for trend data - no charting library dependency.</summary>
    public class TrendGraph : UserControl
    {
        private readonly System.Windows.Shapes.Polyline _line = new()
        {
            Stroke = System.Windows.Media.Brushes.DodgerBlue,
            StrokeThickness = 2
        };
        private readonly System.Windows.Controls.Canvas _canvas = new() { Height = 60, Background = System.Windows.Media.Brushes.WhiteSmoke };
        private readonly System.Collections.Generic.Queue<float> _samples = new();
        private const int MaxSamples = 100;
        private const double Width = 400;

        public TrendGraph(string label)
        {
            var stack = new StackPanel();
            stack.Children.Add(new TextBlock { Text = label, FontSize = 11 });
            _canvas.Children.Add(_line);
            stack.Children.Add(_canvas);
            Content = stack;
        }

        public void AddSample(float value)
        {
            _samples.Enqueue(value);
            while (_samples.Count > MaxSamples) _samples.Dequeue();
            Redraw();
        }

        private void Redraw()
        {
            var points = new System.Windows.Media.PointCollection();
            var arr = _samples.ToArray();
            if (arr.Length == 0) return;

            float max = Math.Max(1f, arr.Length > 0 ? MaxOf(arr) : 1f);
            for (int i = 0; i < arr.Length; i++)
            {
                double x = (double)i / MaxSamples * Width;
                double y = 60 - (arr[i] / max) * 60;
                points.Add(new System.Windows.Point(x, y));
            }
            _line.Points = points;
        }

        private static float MaxOf(float[] arr)
        {
            float m = 0f;
            foreach (var v in arr) if (v > m) m = v;
            return m;
        }
    }
}
