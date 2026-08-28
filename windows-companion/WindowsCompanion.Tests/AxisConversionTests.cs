using Xunit;
using WindowsCompanion;

namespace WindowsCompanion.Tests
{
    public class AxisConversionTests
    {
        [Theory]
        [InlineData(0f, 0)]
        [InlineData(1f, short.MaxValue)]
        [InlineData(-1f, short.MinValue + 1)] // -32768 * MaxValue truncation; close to min
        public void ToShortRange_MapsNormalizedToXboxAxis(float input, int expectedApprox)
        {
            var result = ViGEmVJoyOutput.ToShortRange(input);
            Assert.InRange(result, expectedApprox - 5, expectedApprox + 5);
        }

        [Fact]
        public void ToShortRange_ClampsOutOfRangeInput()
        {
            var result = ViGEmVJoyOutput.ToShortRange(5f);
            Assert.Equal(short.MaxValue, result);
        }

        [Theory]
        [InlineData(0f, 0)]
        [InlineData(1f, 255)]
        [InlineData(0.5f, 127)]
        public void ToByteRange_MapsNormalizedToTriggerByte(float input, int expectedApprox)
        {
            var result = ViGEmVJoyOutput.ToByteRange(input);
            Assert.InRange(result, expectedApprox - 2, expectedApprox + 2);
        }

        [Fact]
        public void ToByteRange_ClampsNegativeInput()
        {
            var result = ViGEmVJoyOutput.ToByteRange(-1f);
            Assert.Equal(0, result);
        }
    }
}
