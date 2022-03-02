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

    private static final Logger log = LoggerFactory.getLogger(HeaterMeterService.class);

    protected static final Duration MIN_PULSE_TIME = Duration.ofMillis(30);   // according to docs: 80ms
    protected static final Duration MAX_PULSE_TIME = Duration.ofMillis(130);  // according to docs: 80ms

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
        return pfcLevel;
    }

    public double getPower() {
        return power;
    }

    private void handleStateChange(PinState state) {
        Instant now = Instant.now();
        Duration pulseTime = Duration.between(lastOn, now);

        if (state == PinState.LOW) {
            if (!isBetween(pulseTime, MIN_PULSE_GAP, MAX_PULSE_GAP)) {
                log.info("Invalid pulse gap: " + pulseTime);
            } else {
                handlePulse(pulseTime);
            }
            lastOn = now;
        }

        if (state == PinState.HIGH) {
            if (!isBetween(pulseTime, MIN_PULSE_TIME, MAX_PULSE_TIME)) {
                log.info("Invalid pulse length: " + pulseTime);
                lastOn = Instant.MIN;
            }
        }
    }

    private void handlePulse(Duration pulseGap) {

        wattHours += 0.5;  // YEM015SD device: 0.5 Wh/impulse

        // 0.5 Wh --> 2000 imp/hour @ 1 kWh --> 1.8sec / impulse

        double timeInSecs = pulseGap.toMillis() / 1000.0;
        power = 1000.0 * 1.8 / timeInSecs;

        // y=(-cos(x*pi)+1)/2 from 0 to 1  --> steady increase from 0 to 1
        // solve for x: 2*asin(sqrt(y))/PI --> not sure why, wolfram alpha solved it like this

        double y = power / 1130.0;
        double x = 2.0 * asin(sqrt(y))/PI;

        int newPfcLevel = (int) (0.5 + 100 * x); // add 0.5 to round properly to closest int

        if (pfcLevel != newPfcLevel) {
            log.info("Consumption: " + (int)power + " W - PFC level: " + pfcLevel);
        }

        pfcLevel = newPfcLevel;

//        10           : 25
//        20           : 93
//        30           : 221
//        40           : 392
//        50           : 577
//        60           : 755
//        70           : 924
//        80           : 1053
//        90           : 1102
//        100          : 1128

    }

    private static boolean isBetween(Duration pulseTime, Duration minPulseTime, Duration maxPulseTime) {
        return pulseTime.compareTo(minPulseTime) > 0 && pulseTime.compareTo(maxPulseTime) < 0;
    }
}
