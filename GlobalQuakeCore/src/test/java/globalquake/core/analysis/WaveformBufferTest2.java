package globalquake.core.analysis;
import org.junit.Test;

import static org.junit.Assert.*;

public class WaveformBufferTest2 {

    @Test
    public void testSize() {
        double sps = 30.0;
        int seconds = 10;
        WaveformBuffer waveformBuffer = new WaveformBuffer(sps, seconds, false);
        assertEquals(300, waveformBuffer.getSize());
    }

    @Test
    public void testRing() {
        double sps = 1.0;
        int seconds = 10;
        WaveformBuffer waveformBuffer = new WaveformBuffer(sps, seconds, false);
        assertEquals(10, waveformBuffer.getSize());
        assertEquals(0, waveformBuffer.getNextSlot());
        assertEquals(0, waveformBuffer.getOldestDataSlot());
        assertTrue(waveformBuffer.isEmpty());

        waveformBuffer.log(0, 0, 0, 0, 0, 1, 0, false);
        assertEquals(1, waveformBuffer.getNextSlot());
        assertEquals(0, waveformBuffer.getOldestDataSlot());

        for (int i = 0; i < 9; i++) {
            waveformBuffer.log(i + 1, 0, 0, 0, 0, 1, 0, false);
        }
        assertEquals(waveformBuffer.getNextSlot(), 0);
        assertEquals(waveformBuffer.getOldestDataSlot(), 0);
        waveformBuffer.log(10, 0, 0, 0, 0, 1, 0, false);
        assertEquals(waveformBuffer.getNextSlot(), 1);
        assertEquals(waveformBuffer.getOldestDataSlot(), 1);
    }

    @Test
    public void testStorage() {
        double sps = 1.0;
        int seconds = 10;
        WaveformBuffer waveformBuffer = new WaveformBuffer(sps, seconds, false);
        waveformBuffer.log(0, 10, 10, 10, 10, 1, 10, false);

        Log log0 = waveformBuffer.toLog(0);

        assertEquals(log0.time(), 0);
        assertEquals(log0.ratio(), 10, 1e-6);
    }

    @Test
    public void testResize() {
        double sps = 1.0;
        int seconds = 10;
        WaveformBuffer waveformBuffer = new WaveformBuffer(sps, seconds, false);

        for (int i = 0; i < waveformBuffer.getSize(); i++) {
            waveformBuffer.log(i * 1000L, i * 10, i * 20, i * 30, i * 40, 1, i * 60, false);
        }

        waveformBuffer.resize(3);

        assertEquals(3, waveformBuffer.getSize());

        Log log0 = waveformBuffer.toLog(waveformBuffer.getOldestDataSlot());

        assertEquals(7000, log0.time());
        assertEquals(30 * 7, log0.ratio(), 1e-6);

        waveformBuffer.log(10000, 100, 100, 100, 100, 1, 100, false);

        log0 = waveformBuffer.toLog(waveformBuffer.getOldestDataSlot());

        assertEquals(8000, log0.time());
        assertEquals(30 * 8, log0.ratio(), 1e-6);

        log0 = waveformBuffer.toLog(waveformBuffer.getNewestDataSlot());

        assertEquals(10000, log0.time());
        assertEquals(100, log0.ratio(), 1e-6);
    }

    @Test
    public void testExpand() {
        WaveformBuffer waveformBuffer = new WaveformBuffer(1, 3, false);
        assertEquals(3, waveformBuffer.getSize());
        for (int i = 0; i < waveformBuffer.getSize(); i++) {
            waveformBuffer.log(i * 1000L, i, i, i, i, i, i, false);
        }

        assertEquals(0, waveformBuffer.getTime(waveformBuffer.getOldestDataSlot()));
        waveformBuffer.log(3000, 3, 3, 3, 3, 3, 3, true);
        assertEquals(6, waveformBuffer.getSize());
        assertEquals(0, waveformBuffer.getTime(waveformBuffer.getOldestDataSlot()));
        assertEquals(3000, waveformBuffer.getTime(waveformBuffer.getNewestDataSlot()));

    }

    @Test
    public void testEmptyBuffer() {
        WaveformBuffer buffer = new WaveformBuffer(10, 10, false);
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.getNextSlot());
        assertEquals(0, buffer.getOldestDataSlot());
    }

    @Test
    public void testSingleElementBuffer() {
        WaveformBuffer buffer = new WaveformBuffer(1, 1, false);
        buffer.log(0, 1, 2, 3, 4, 5, false);
        assertEquals(1, buffer.getSize());
        assertEquals(0, buffer.getNextSlot());
        assertEquals(0, buffer.getOldestDataSlot());
        assertFalse(buffer.isEmpty());
    }

    @Test
    public void testResizeToSmallerSize() {
        WaveformBuffer buffer = new WaveformBuffer(1, 10, false);
        // Fill the buffer
        for (int i = 0; i < 10; i++) {
            buffer.log(i * 1000, i, i, i, i, i, false);
        }

        buffer.resize(5);
        assertEquals(5, buffer.getSize());
        // Verify that the last 5 elements are preserved
        for (int i = 5; i < 10; i++) {
            assertEquals(buffer.getTime(buffer.getOldestDataSlot() + i - 5), i * 1000);
        }
    }

    @Test
    public void testTimeReferenceOverflow() {
        WaveformBuffer buffer = new WaveformBuffer(1, 1000, false);
        // Fill the buffer with timestamps exceeding TIME_REF_LIMIT
        for (int i = 0; i < 1000; i++) {
            buffer.log(2_000_000_000 + i * 1000, i, i, i, i, i, false);
        }
        // Verify that the time differences are calculated correctly
        for (int i = 1; i < 1000; i++) {
            assertEquals(1000, buffer.getTime(i) - buffer.getTime(i - 1));
        }
    }

    // the extract should return 3000, 4000, 5000 , 6000, 7000
    // but returns 4000,5000,6000,7000,4000
    // something weird going on
    @Test
    public void testExtract() {
        WaveformBuffer buffer = new WaveformBuffer(1, 10, false);
        // Fill the buffer
        for (int i = 0; i < 10; i++) {
            buffer.log(i * 1000, i, i, i, i, i, false);
        }

        WaveformBuffer extracted = buffer.extract(3000, 7000);
        assertEquals(5, extracted.getSize());
        // Verify extracted data
        for (int i = 0; i <= 4; i++) {
            System.out.println(extracted.getTime(i));
            assertEquals(3000 + i * 1000,extracted.getTime(i) );
        }
    }
    //FIX: WHEN TRYING TO EXTRACT DATA OUTSIDE THE RANGE IT SHOULD RETURN EMPTY BUFFER
    @Test
    public void testExtractNoDataInRange() {
        WaveformBuffer buffer = new WaveformBuffer(1, 10, false);
        // Fill the buffer with data
        for (int i = 0; i < 10; i++) {
            buffer.log(i * 1000, i, i, i, i, i, false);
        }

        // Attempt to extract data from a range that has no corresponding logs
        WaveformBuffer extracted = buffer.extract(10000, 11000);
        assertEquals(0, extracted.getSize()); // Expecting an empty buffer
    }

    //FIX: CHECK IF THE NEW LOG IS NEWER THAN THE ONE BEFORE ELSE IT FAILS
    @Test
    public void testLogWithDecreasingTime() {
        WaveformBuffer buffer = new WaveformBuffer(1, 10, false);
        buffer.log(0, 1, 1, 1, 1, 1, false); // First log
        buffer.log(1000, 2, 2, 2, 2, 2, false); // Second log

        // Attempt to log with a time earlier than the last log
        buffer.log(500, 3, 3, 3, 3, 3, false); // This should not change the buffer

        // Verify that the size and the last log remain unchanged
        assertEquals(2, buffer.getSize());
        assertEquals(1000, buffer.getTime(buffer.getNewestDataSlot())); // Last log time should still be 1000
    }




}