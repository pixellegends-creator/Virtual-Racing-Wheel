using Xunit;
using WindowsCompanion;

namespace WindowsCompanion.Tests
{
    public class CompanionEngineTests
    {
        private CompanionEngine CreateEngine() => new CompanionEngine(new NullControllerOutput());

        [Fact]
        public void RequestPairing_CreatesUntrustedDeviceWithPin()
        {
            var engine = CreateEngine();
            var device = engine.RequestPairing("dev1", "192.168.1.50");

            Assert.False(device.Trusted);
            Assert.Equal(4, device.Pin.Length);
            Assert.False(engine.IsTrusted("dev1"));
        }

        [Fact]
        public void ConfirmPairing_WithCorrectPin_TrustsDevice()
        {
            var engine = CreateEngine();
            var device = engine.RequestPairing("dev1", "192.168.1.50");

            var result = engine.ConfirmPairing("dev1", device.Pin);

            Assert.True(result);
            Assert.True(engine.IsTrusted("dev1"));
        }

        [Fact]
        public void ConfirmPairing_WithWrongPin_DoesNotTrustDevice()
        {
            var engine = CreateEngine();
            engine.RequestPairing("dev1", "192.168.1.50");

            var result = engine.ConfirmPairing("dev1", "0000");

            Assert.False(result);
            Assert.False(engine.IsTrusted("dev1"));
        }

        [Fact]
        public void ConfirmPairing_UnknownDevice_ReturnsFalse()
        {
            var engine = CreateEngine();
            var result = engine.ConfirmPairing("nonexistent", "1234");
            Assert.False(result);
        }

        [Fact]
        public void RevokeDevice_RemovesTrust()
        {
            var engine = CreateEngine();
            var device = engine.RequestPairing("dev1", "192.168.1.50");
            engine.ConfirmPairing("dev1", device.Pin);

            var revoked = engine.RevokeDevice("dev1");

            Assert.True(revoked);
            Assert.False(engine.IsTrusted("dev1"));
        }

        [Fact]
        public void RevokeDevice_UnknownDevice_ReturnsFalse()
        {
            var engine = CreateEngine();
            Assert.False(engine.RevokeDevice("nonexistent"));
        }

        [Fact]
        public void GeneratePin_IsFourDigits()
        {
            var engine = CreateEngine();
            for (int i = 0; i < 20; i++)
            {
                var pin = engine.GeneratePin();
                Assert.Equal(4, pin.Length);
                Assert.True(int.TryParse(pin, out _));
            }
        }

        [Fact]
        public void ApplyControlFrame_ForwardsToControllerOutput()
        {
            var output = new RecordingControllerOutput();
            var engine = new CompanionEngine(output);
            var frame = new ControlFrame { Steering = 0.5f, Throttle = 1f };

            engine.ApplyControlFrame(frame);

            Assert.Equal(1, output.UpdateCallCount);
            Assert.Equal(0.5f, output.LastFrame.Steering);
        }

        private class RecordingControllerOutput : IVirtualControllerOutput
        {
            public int UpdateCallCount;
            public ControlFrame LastFrame;

            public void Update(ControlFrame frame)
            {
                UpdateCallCount++;
                LastFrame = frame;
            }

            public void Reset() { }
        }
    }
}
