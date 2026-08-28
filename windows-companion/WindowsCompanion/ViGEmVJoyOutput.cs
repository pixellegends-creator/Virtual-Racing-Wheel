using System;
using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;

namespace WindowsCompanion
{
    /// <summary>
    /// Drives a virtual Xbox 360 controller via ViGEmBus for steering/throttle/brake, and a
    /// separate vJoy device for a dedicated clutch axis - vJoy was added because ViGEmBus's
    /// Xbox 360 target has no clutch-appropriate axis; running both side by side gives games
    /// that support a third pedal a proper dedicated input instead of overloading the right stick.
    /// </summary>
    public class ViGEmVJoyOutput : IVirtualControllerOutput, IDisposable
    {
        private readonly ViGEmClient _client;
        private readonly IXbox360Controller _pad;
        private readonly IVJoyClutchDevice _vJoyClutch;

        public ViGEmVJoyOutput(IVJoyClutchDevice vJoyClutch)
        {
            _client = new ViGEmClient();
            _pad = _client.CreateXbox360Controller();
            _pad.Connect();
            _vJoyClutch = vJoyClutch;
            _vJoyClutch.Acquire();
        }

        public void Update(ControlFrame frame)
        {
            // Xbox 360 thumbstick range is -32768..32767.
            short steeringAxis = ToShortRange(frame.Steering);
            _pad.SetAxisValue(Xbox360Axis.LeftThumbX, steeringAxis);

            // Triggers are 0..255.
            byte throttleByte = ToByteRange(frame.Throttle);
            byte brakeByte = ToByteRange(frame.Brake);
            _pad.SetSliderValue(Xbox360Slider.RightTrigger, throttleByte);
            _pad.SetSliderValue(Xbox360Slider.LeftTrigger, brakeByte);

            short camX = ToShortRange(frame.CameraX);
            short camY = ToShortRange(frame.CameraY);
            _pad.SetAxisValue(Xbox360Axis.RightThumbX, camX);
            _pad.SetAxisValue(Xbox360Axis.RightThumbY, camY);

            _pad.SubmitReport();

            _vJoyClutch.SetClutchAxis(frame.Clutch);
        }

        public void Reset()
        {
            _pad.SetAxisValue(Xbox360Axis.LeftThumbX, 0);
            _pad.SetSliderValue(Xbox360Slider.RightTrigger, 0);
            _pad.SetSliderValue(Xbox360Slider.LeftTrigger, 0);
            _pad.SubmitReport();
            _vJoyClutch.SetClutchAxis(0f);
        }

        public static short ToShortRange(float normalized)
        {
            var clamped = Math.Clamp(normalized, -1f, 1f);
            return (short)(clamped * short.MaxValue);
        }

        public static byte ToByteRange(float normalized01)
        {
            var clamped = Math.Clamp(normalized01, 0f, 1f);
            return (byte)(clamped * byte.MaxValue);
        }

        public void Dispose()
        {
            _pad.Disconnect();
            _vJoyClutch.Release();
            _client.Dispose();
        }
    }

    public interface IVJoyClutchDevice
    {
        void Acquire();
        void SetClutchAxis(float normalized01);
        void Release();
    }
}
