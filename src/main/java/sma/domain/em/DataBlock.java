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
        this.data = data;
    }

    public String getSerialNumber() {
        return String.valueOf(serialNo.getValue(data));
    }

    public float getPowerIn() {
        return (powerIn.getValue(data));
    }

    public float getPowerOut() {
        return (powerOut.getValue(data));
    }

    public float getEnergyIn() {
        return (energyIn.getValue(data));
    }

    public float getEnergyOut() {
        return (energyOut.getValue(data));
    }

    public float getPowerInL1() {
        return (powerInL1.getValue(data));
    }

    public float getPowerOutL1() {
        return (powerOutL1.getValue(data));
    }

    public float getEnergyInL1() {
        return (energyInL1.getValue(data));
    }

    public float getEnergyOutL1() {
        return (energyOutL1.getValue(data));
    }

    public float getPowerInL2() {
        return (powerInL2.getValue(data));
    }

    public float getPowerOutL2() {
        return (powerOutL2.getValue(data));
    }

    public float getEnergyInL2() {
        return (energyInL2.getValue(data));
    }

    public float getEnergyOutL2() {
        return (energyOutL2.getValue(data));
    }

    public float getPowerInL3() {
        return (powerInL3.getValue(data));
    }

    public float getPowerOutL3() {
        return (powerOutL3.getValue(data));
    }

    public float getEnergyInL3() {
        return (energyInL3.getValue(data));
    }

    public float getEnergyOutL3() {
        return (energyOutL3.getValue(data));
    }


}
