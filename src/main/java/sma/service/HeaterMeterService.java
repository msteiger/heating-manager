package sma.service;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiGpioProvider;
import com.pi4j.io.gpio.RaspiPinNumberingScheme;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import static java.lang.Math.*;

public class HeaterMeterService {

    private static final Logger log = LoggerFactory.getLogger(HeaterMeterService.class);

    protected static final Duration MIN_PULSE_TIME = Duration.ofMillis(30);   // according to docs: 80ms
    protected static final Duration MAX_PULSE_TIME = Duration.ofMillis(130);  // according to docs: 80ms

    protected static final Duration MIN_PULSE_GAP = Duration.ofMillis(100);
    protected static final Duration MAX_PULSE_GAP = Duration.ofHours(1);

    private Instant lastOn = Instant.MIN;

    private double wattHours = 0;
    private Instant startTime = Instant.now();

    public HeaterMeterService(Pin pinSwitchOnOff) {
        GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
        GpioController controller = GpioFactory.getInstance();

        GpioPinDigitalInput switchGpio = controller.provisionDigitalInputPin(pinSwitchOnOff, PinPullResistance.PULL_UP);

        switchGpio.addListener(new GpioPinListenerDigital() {

            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                PinState state = event.getState();
                handleStateChange(state);
            }
        });
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
        double power = 1000.0 * 1.8 / timeInSecs;

        int pfcLevel = (int)(power / 1130.0 * 100);

        // y=(-cos(x*pi)+1)/2 from 0 to 1  --> steady increase from 0 to 1
        // solve for x: 2*asin(sqrt(y))/PI

        double y = power / 1130.0;

        int sinPfcLevel = (int) (100 * 2.0 * asin(sqrt(y))/PI);

//        log.info("Consumption: " + (int)power + " W - PFC levels: " + pfcLevel + "/" + sinPfcLevel);
    }

    private static boolean isBetween(Duration pulseTime, Duration minPulseTime, Duration maxPulseTime) {
        return pulseTime.compareTo(minPulseTime) > 0 && pulseTime.compareTo(maxPulseTime) < 0;
    }
}
