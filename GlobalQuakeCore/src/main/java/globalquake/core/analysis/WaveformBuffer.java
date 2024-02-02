package globalquake.core.analysis;

import org.tinylog.Logger;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WaveformBuffer {
    public static final int COMPUTED_COUNT_CLIENT = 4;
    public static final int COMPUTED_COUNT_SERVER = 3;
    public static final int RATIO = 0;
    public static final int MEDIUM_RATIO = 1;
    public static final int SPECIAL_RATIO = 2;
    public static final int FILTERED_VALUE = 3;
    private static final double GAP_TOLERANCE = 0.05;
    private final double sps;
    private final Lock readLock;
    private final Lock writeLock;
    private final boolean server;
    private double actualSampleTime;
    private final double expectedGap;


    private int size;
    private long lastLog;
    private int[] rawValues;
    private float[][] computed;

    private int nextFreeSlot;
    private int oldestDataSlot;

    public WaveformBuffer(double sps, int seconds, boolean server) {
        this.server = server;
        this.sps = sps;
        this.size = (int) Math.ceil(seconds * sps);

        if(size <= 0){
            throw new IllegalArgumentException("Wavefor buffer size must be positive!");
        }

        if(!isServer()) {
            rawValues = new int[size];
        }

        computed = new float[getComputedCount()][size];
        this.lastLog = Long.MIN_VALUE;
        this.nextFreeSlot = 0;
        this.oldestDataSlot = 0;
        this.expectedGap = (1000.0 / sps);
        this.actualSampleTime = expectedGap;

        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
    }

    private void log(long time, int rawValue, float filteredV, float ratio, float mediumRatio, float specialRatio, boolean expand) {
        if(time <= lastLog) {
            return;
        }
        if(expand && !isEmpty() && nextFreeSlot == oldestDataSlot){
            _resize(size * 2);
        }

        if(!isServer()){
            rawValues[nextFreeSlot] = rawValue;
            computed[FILTERED_VALUE][nextFreeSlot] = filteredV;

        }

        computed[RATIO][nextFreeSlot] = ratio;
        computed[MEDIUM_RATIO][nextFreeSlot] = mediumRatio;
        computed[SPECIAL_RATIO][nextFreeSlot] = specialRatio;

        if (nextFreeSlot == oldestDataSlot && !isEmpty()) {
            oldestDataSlot = (oldestDataSlot + 1) % size;
        }
        nextFreeSlot = (nextFreeSlot + 1) % size;

        if(lastLog != Long.MIN_VALUE){
            long gap = time - lastLog;
            double diff = Math.abs(gap - expectedGap) / expectedGap;
            if(diff < GAP_TOLERANCE) {
                actualSampleTime -= (actualSampleTime - gap) / (sps * 200.0);
            }
        }
        lastLog = time;
    }

    public void log(long time, int rawValue, float filteredV, float shortAverage, float mediumAverage, float longAverage,
                    float specialAverage, boolean expand){
        log(time, rawValue, filteredV, shortAverage / longAverage, mediumAverage / longAverage, specialAverage / longAverage, expand);
    }

    public void resize(int seconds) {
        int new_size = (int) Math.ceil(seconds * sps);
        _resize(new_size);
    }

    private void _resize(int new_size) {
        int[] new_rawValues = isServer() ? null : new int[new_size];
        float[][] new_computed = new float[getComputedCount()][new_size];

        int i2 = 0;
        for(int step = 0; step < Math.min(size, new_size); step++){
            i2 -= 1;
            if(i2 < 0){
                i2 = new_size - 1;
            }

            nextFreeSlot -= 1;
            if(nextFreeSlot < 0){
                nextFreeSlot = size - 1;
            }

            if(!isServer()){
                new_rawValues[i2] = rawValues[nextFreeSlot];
            }

            for(int i = 0; i < getComputedCount(); i++) {
                new_computed[i][i2] = computed[i][nextFreeSlot];
            }
        }

        this.rawValues = new_rawValues;
        this.computed = new_computed;

        this.oldestDataSlot = i2;
        this.nextFreeSlot = 0;
        this.size = new_size;
    }

    private int getComputedCount() {
        return isServer() ? COMPUTED_COUNT_SERVER : COMPUTED_COUNT_CLIENT;
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return lastLog == Long.MIN_VALUE;
    }

    public int getNextSlot() {
        return nextFreeSlot;
    }

    public int getOldestDataSlot() {
        return oldestDataSlot;
    }

    public long getTime(int index){
        int newest=getNewestDataSlot();
        if(index > newest){
            index -= size;
        }
        return lastLog - (long)(actualSampleTime * (newest - index));
    }

    public int getRaw(int index){
        return rawValues[index];
    }

    public float getComputed(int type, int index){
        return computed[type][index];
    }

    public double getMediumRatio(int index){
        return getComputed(MEDIUM_RATIO, index);
    }


    public double getSpecialRatio(int index){
        return getComputed(SPECIAL_RATIO, index);
    }


    public double getRatio(int index){
        return getComputed(RATIO, index);
    }

    public Log toLog(int index){
        if(isServer()){
            throw new UnsupportedOperationException("toLog() is not supported in server mode!");
        }
        return new Log(
                getTime(index),
                isServer() ? 0 : rawValues[index],
                isServer() ? 0 : computed[FILTERED_VALUE][index],
                computed[RATIO][index],
                computed[MEDIUM_RATIO][index],
                computed[SPECIAL_RATIO][index]);
    }

    public Lock getReadLock() {
        return readLock;
    }

    public Lock getWriteLock() {
        return writeLock;
    }

    public int getNewestDataSlot() {
        int res = nextFreeSlot - 1;
        return res >= 0 ? res : size - 1;
    }

    public WaveformBuffer extract(long start, long end) {
        int seconds = (int) Math.ceil((end - start) / 1000.0);
        if(seconds <= 0){
            throw new IllegalArgumentException("Cannot extract empty waveform buffer!");
        }

        // additional space
        seconds  = (int)(seconds * 1.4);

        WaveformBuffer result = new WaveformBuffer(sps, seconds, server);

        if(isEmpty()){
            return result;
        }

        int closest = getClosestIndex(start);
        long time = getTime(closest);

        while(closest != getNextSlot() && time <= end){
            result.log(
                    time,
                    getRaw(closest),
                    getComputed(0, closest),
                    getComputed(1, closest),
                    getComputed(2, closest),
                    getComputed(3, closest),
                    true);

            closest = (closest + 1) % size;

            time = getTime(closest);
        }

        return result;
    }

    public int getClosestIndex(long time) {
        if(isEmpty()){
            throw new IllegalStateException("There is no closest log since the buffer is empty!");
        }

        int low = getOldestDataSlot();
        int high = getNewestDataSlot();
        if(low > high){
            high += size;
        }

        while(high - low > 1){
            int mid = (low + high) / 2;
            if(getTime(mid % size) > time){
                high = mid;
            } else{
                low = mid;
            }
        }
        return high % size;
    }

    public boolean isServer() {
        return server;
    }
}