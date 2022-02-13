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

    public float getValue(byte[] bytes) {
        if (length == 4) {
            return (float) bytesToUInt16(Arrays.copyOfRange(bytes, address, address + 4)) / divisor;
        } else {
            return (float) bytesToUInt32(Arrays.copyOfRange(bytes, address, address + 8)) / divisor;
        }
    }

    private static int bytesToUInt16(byte[] bytes) {
        return Ints.fromByteArray(bytes);
    }

    private static long bytesToUInt32(byte[] bytes) {
        return Longs.fromByteArray(bytes);
    }
}
