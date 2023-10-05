package sma.domain.em;

import java.util.Arrays;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * This class holds the data for a single field
 */
public class FieldDTO {

    private final int address;
    private final int length;
    private final int divisor;

    public FieldDTO(int address, int length, int divisor) {
        this.address = address;
        if ((length != 4) && (length != 8)) {
            throw new IllegalArgumentException("length should be 4 or 8 bytes");
        }
        this.length = length;
        this.divisor = divisor;
    }

    public int getAddress() {
        return address;
    }

    public int getDivisor() {
        return divisor;
    }

    public int getLength() {
        return length;
    }

    public long getValueLong(byte[] bytes) {
        if (divisor != 1) {
            throw new IllegalArgumentException("must be an integer field");
        }
        return getValueInternal(bytes);
    }

    public float getValueFloat(byte[] bytes) {
        return getValueInternal(bytes) / (float)divisor;
    }

    private long getValueInternal(byte[] bytes) {
        if (length == 4) {
            return bytesToUInt32(Arrays.copyOfRange(bytes, address, address + 4));
        } else if (length == 8) {
            return bytesToUInt64(Arrays.copyOfRange(bytes, address, address + 8));
        } else {
            throw new IllegalArgumentException("length must be 4 or 8");
        }
    }

    private static int bytesToUInt32(byte[] bytes) {
        return Ints.fromByteArray(bytes);
    }

    private static long bytesToUInt64(byte[] bytes) {
        return Longs.fromByteArray(bytes);
    }
}
