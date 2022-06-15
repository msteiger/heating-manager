package sma.service;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.sqrt;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class HeaterMeterService {

    private static final double PULSE_PER_WH = 0.5;  // YEM015SD device: 0.5 Wh/impulse

    private static final double MAX_POWER = 1130.0;  // 1200 Watt according to docs

    private static final Logger log = LoggerFactory.getLogger(HeaterMeterService.class);

    protected static final Duration MIN_PULSE_TIME = Duration.ofMillis(25);   // according to docs: 80ms
    protected static final Duration MAX_PULSE_TIME = Duration.ofMillis(135);  // according to docs: 80ms

    protected static final Duration MIN_PULSE_GAP = Duration.ofMillis(100);
    protected static final Duration MAX_PULSE_GAP = Duration.ofHours(1);

    private Instant lastOn = Instant.MIN;

    private volatile double power = 0;
    private volatile double wattHours = 0;
    private volatile int pfcLevel;
    private volatile Instant startTime = Instant.now();

    public HeaterMeterService(GpioController controller, Pin sZeroBusInput) {

        GpioPinDigitalInput switchGpio = controller.provisionDigitalInputPin(sZeroBusInput, PinPullResistance.PULL_UP);

        switchGpio.addListener(new GpioPinListenerDigital() {

            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                PinState state = event.getState();
                handleStateChange(state);
            }
        });
    }

    public double getWattHours() {
        return wattHours;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public int getPfcLevel() {
        int maxPfcLevel = getMaxPfcLevel(); // maximum possible level (if next pulse comes now)

        // if time to last pulse is larger than the time between the last two pulses, the PFC level must be lower
        return pfcLevel < maxPfcLevel ? pfcLevel : maxPfcLevel;
    }

    public double getPower() {
        double maxPower = findMaxPower();

        // if time to last pulse is larger than the time between the last two pulses, the power value must be lower
        return power < maxPower ? power : maxPower;
    }

    public int getRawPfcLevel() {
        return pfcLevel;
    }

    public int getMaxPfcLevel() {
        double maxPower = findMaxPower();
        return powerToPfcLevel(maxPower);
    }

    public double getRawPower() {
        return power;
    }

    private void handleStateChange(PinState state) {
        Instant now = Instant.now();
        Duration pulseTime = Duration.between(lastOn, now);

        if (state == PinState.LOW) {
            if (!isBetween(pulseTime, MIN_PULSE_GAP, MAX_PULSE_GAP)) {
                if (!Instant.MIN.equals(lastOn)) {
                    log.info("Invalid pulse gap: " + pulseTime.toMillis() + " ms.");
                }
            } else {
                handlePulse(pulseTime);
            }
            lastOn = now;
        }

        if (state == PinState.HIGH) {
            if (!isBetween(pulseTime, MIN_PULSE_TIME, MAX_PULSE_TIME)) {
                log.info("Invalid pulse length: " + pulseTime);
                lastOn = Instant.MIN;  // reset
            }
        }
    }

    private void handlePulse(Duration pulseGap) {

        wattHours += PULSE_PER_WH;

        double timeInSecs = pulseGap.toMillis() / 1000.0;

        double newPower = durationToPower(timeInSecs);

        if (newPower > MAX_POWER) {
            log.info("Too much power consumption: {}/{}", newPower, MAX_POWER);
        }

        int newPfcLevel = powerToPfcLevel(newPower);

        if (pfcLevel != newPfcLevel) {
            log.debug("Consumption: " + (int)newPower + " W - PFC level: " + newPfcLevel);
        }

        pfcLevel = newPfcLevel;
        power = newPower;
    }

    private double findMaxPower() {
        if (Instant.MIN.equals(lastOn)) {
            return 0;
        }

        Instant now = Instant.now();
        Duration pulseTime = Duration.between(lastOn, now);

        double timeInSecs = pulseTime.toMillis() / 1000.0;
        double maxPower = durationToPower(timeInSecs);

        return maxPower;
    }

    private static double durationToPower(double timeInSecs) {
        // 0.5 Wh --> 2000 imp/hour @ 1 kWh --> 1.8sec / impulse
        double newPower = 3600.0 * PULSE_PER_WH / timeInSecs;
        return newPower;
    }

    private static int powerToPfcLevel(double newPower) {

        // y=(-cos(x*pi)+1)/2 from 0 to 1  --> steady increase from 0 to 1
        // solve for x: 2*asin(sqrt(y))/PI --> not sure why, wolfram alpha solved it like this

        //   10   :   25
        //   20   :   93
        //   30   :  221
        //   40   :  392
        //   50   :  577
        //   60   :  755
        //   70   :  924
        //   80   : 1053
        //   90   : 1102
        //  100   : 1128

        double y = Math.min(1.0, newPower / MAX_POWER);
        double x = 2.0 * asin(sqrt(y))/PI;

        int newPfcLevel = (int) (0.5 + 100 * x); // add 0.5 to round properly to closest int
        return newPfcLevel;
    }

    private static boolean isBetween(Duration pulseTime, Duration minPulseTime, Duration maxPulseTime) {
        return pulseTime.compareTo(minPulseTime) > 0 && pulseTime.compareTo(maxPulseTime) < 0;
    }
}
