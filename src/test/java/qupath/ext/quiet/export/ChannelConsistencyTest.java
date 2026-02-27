package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import qupath.lib.images.servers.ImageChannel;

/**
 * Tests for channel signature consistency logic.
 * <p>
 * The {@code BatchExportTask.channelSignature()} method computes a string
 * from channel names and colors. These tests verify the signature format
 * and consistency properties using the same algorithm.
 */
class ChannelConsistencyTest {

    /**
     * Compute a channel signature from a list of channels, matching the
     * logic in {@code BatchExportTask.channelSignature()}.
     */
    private static String channelSignature(List<ImageChannel> channels) {
        var sb = new StringBuilder();
        for (var ch : channels) {
            sb.append(ch.getName()).append('|')
              .append(Integer.toHexString(ch.getColor())).append(',');
        }
        return sb.toString();
    }

    @Test
    void testConsistentChannels() {
        var channels1 = List.of(
                ImageChannel.getInstance("Red", 0xFF0000),
                ImageChannel.getInstance("Green", 0x00FF00),
                ImageChannel.getInstance("Blue", 0x0000FF));
        var channels2 = List.of(
                ImageChannel.getInstance("Red", 0xFF0000),
                ImageChannel.getInstance("Green", 0x00FF00),
                ImageChannel.getInstance("Blue", 0x0000FF));

        String sig1 = channelSignature(channels1);
        String sig2 = channelSignature(channels2);

        assertEquals(sig1, sig2,
                "Servers with the same channels should produce the same signature");
    }

    @Test
    void testInconsistentChannels() {
        var channels1 = List.of(
                ImageChannel.getInstance("Red", 0xFF0000),
                ImageChannel.getInstance("Green", 0x00FF00),
                ImageChannel.getInstance("Blue", 0x0000FF));
        var channels2 = List.of(
                ImageChannel.getInstance("DAPI", 0x0000FF),
                ImageChannel.getInstance("FITC", 0x00FF00),
                ImageChannel.getInstance("Cy3", 0xFF0000));

        String sig1 = channelSignature(channels1);
        String sig2 = channelSignature(channels2);

        assertNotEquals(sig1, sig2,
                "Servers with different channels should produce different signatures");
    }

    @Test
    void testSingleChannel() {
        var channels = List.of(ImageChannel.getInstance("Brightfield", 0xFFFFFF));
        String sig = channelSignature(channels);

        assertNotNull(sig, "Single channel should produce a valid signature");
        assertFalse(sig.isEmpty(), "Signature should not be empty");
    }

    @Test
    void testChannelOrderMatters() {
        var channels1 = List.of(
                ImageChannel.getInstance("Red", 0xFF0000),
                ImageChannel.getInstance("Green", 0x00FF00),
                ImageChannel.getInstance("Blue", 0x0000FF));
        var channels2 = List.of(
                ImageChannel.getInstance("Blue", 0x0000FF),
                ImageChannel.getInstance("Green", 0x00FF00),
                ImageChannel.getInstance("Red", 0xFF0000));

        String sig1 = channelSignature(channels1);
        String sig2 = channelSignature(channels2);

        assertNotEquals(sig1, sig2,
                "Different channel ordering should produce different signatures");
    }

    @Test
    void testEmptyChannels() {
        var channels = List.<ImageChannel>of();
        String sig = channelSignature(channels);

        assertNotNull(sig, "Empty channel list should produce a valid signature");
        assertEquals("", sig, "Empty channel list should produce empty signature");
    }

    @Test
    void testSignatureDeterministic() {
        var channels = List.of(
                ImageChannel.getInstance("DAPI", 0x0000FF),
                ImageChannel.getInstance("FITC", 0x00FF00),
                ImageChannel.getInstance("Cy3", 0xFF0000),
                ImageChannel.getInstance("Cy5", 0xFF00FF));

        String sig1 = channelSignature(channels);
        String sig2 = channelSignature(channels);

        assertEquals(sig1, sig2, "Same channels should always produce the same signature");
    }

    @Test
    void testColorDifferenceCausesInconsistency() {
        var channels1 = List.of(
                ImageChannel.getInstance("Channel1", 0xFF0000));
        var channels2 = List.of(
                ImageChannel.getInstance("Channel1", 0x00FF00));

        String sig1 = channelSignature(channels1);
        String sig2 = channelSignature(channels2);

        assertNotEquals(sig1, sig2,
                "Same name but different color should produce different signatures");
    }

    @Test
    void testSignatureContainsNameAndColor() {
        var channels = List.of(ImageChannel.getInstance("DAPI", 0x0000FF));
        String sig = channelSignature(channels);

        assertTrue(sig.contains("DAPI"), "Signature should contain channel name");
        assertTrue(sig.contains("ff"), "Signature should contain hex color");
    }
}
