package sma.domain.em;

public class DataBlock {

    private static final FieldDTO serialNo = new FieldDTO(0x14, 4, 1);
    private static final FieldDTO powerIn = new FieldDTO(0x20, 4, 10);
    private static final FieldDTO energyIn = new FieldDTO(0x28, 8, 3600000);
    private static final FieldDTO powerOut = new FieldDTO(0x34, 4, 10);
    private static final FieldDTO energyOut = new FieldDTO(0x3C, 8, 3600000);
    private static final FieldDTO powerInL1 = new FieldDTO(0xA8, 4, 10);
    private static final FieldDTO energyInL1 = new FieldDTO(0xB0, 8, 3600000); // +8
    private static final FieldDTO powerOutL1 = new FieldDTO(0xBC, 4, 10); // + C
    private static final FieldDTO energyOutL1 = new FieldDTO(0xC4, 8, 3600000); // +8
    private static final FieldDTO powerInL2 = new FieldDTO(0x138, 4, 10);
    private static final FieldDTO energyInL2 = new FieldDTO(0x140, 8, 3600000); // +8
    private static final FieldDTO powerOutL2 = new FieldDTO(0x14C, 4, 10); // + C
    private static final FieldDTO energyOutL2 = new FieldDTO(0x154, 8, 3600000); // +8
    private static final FieldDTO powerInL3 = new FieldDTO(0x1C8, 4, 10);
    private static final FieldDTO energyInL3 = new FieldDTO(0x1D0, 8, 3600000); // +8
    private static final FieldDTO powerOutL3 = new FieldDTO(0x1DC, 4, 10); // + C
    private static final FieldDTO energyOutL3 = new FieldDTO(0x1E4, 8, 3600000); // +8

    private byte[] data;

    public DataBlock(byte[] data) {
        int minLength = energyOutL3.getAddress() + energyOutL3.getLength();
        if (data == null) {
            throw new IllegalArgumentException("Data block must not be null");
        }
        if (data.length < minLength) {
            throw new IllegalArgumentException("Data block too short: " + data.length + " < " + minLength);
        }
        this.data = data;
    }

    public static DataBlock fromHexString(String hex) {
        if (hex == null || hex.isEmpty() || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("invalid hex string");
        }

        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length() - 1; i += 2) {
            byte b = (byte) Integer.parseInt(hex, i, i + 2, 16);
            data[i / 2] = b;
        }
        return new DataBlock(data);
    }

    public long getSerialNumber() {
        return serialNo.getValueLong(data);
    }

    public float getPowerIn() {
        // the value sometimes is "-206864000"
        return (powerIn.getValueFloat(data));
    }

    public float getPowerOut() {
        return (powerOut.getValueFloat(data));
    }

    public float getEnergyIn() {
        return (energyIn.getValueFloat(data));
    }

    public float getEnergyOut() {
        return (energyOut.getValueFloat(data));
    }

    public float getPowerInL1() {
        return (powerInL1.getValueFloat(data));
    }

    public float getPowerOutL1() {
        return (powerOutL1.getValueFloat(data));
    }

    public float getEnergyInL1() {
        return (energyInL1.getValueFloat(data));
    }

    public float getEnergyOutL1() {
        return (energyOutL1.getValueFloat(data));
    }

    public float getPowerInL2() {
        return (powerInL2.getValueFloat(data));
    }

    public float getPowerOutL2() {
        return (powerOutL2.getValueFloat(data));
    }

    public float getEnergyInL2() {
        return (energyInL2.getValueFloat(data));
    }

    public float getEnergyOutL2() {
        return (energyOutL2.getValueFloat(data));
    }

    public float getPowerInL3() {
        return (powerInL3.getValueFloat(data));
    }

    public float getPowerOutL3() {
        return (powerOutL3.getValueFloat(data));
    }

    public float getEnergyInL3() {
        return (energyInL3.getValueFloat(data));
    }

    public float getEnergyOutL3() {
        return (energyOutL3.getValueFloat(data));
    }

    @Override
    public String toString() {
        return "DataBlock [" + byteArrayToHex(data) + "]";
    }

    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a) {
           sb.append(String.format("%02x", b));
        }
        return sb.toString();
     }
}
