package sma.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiGpioProvider;
import com.pi4j.io.gpio.RaspiPinNumberingScheme;
import com.pi4j.io.gpio.SimulatedGpioProvider;

import sma.HeatingManagerApplication;

/**
 * TODO: describe
 */
public class Heater {

    private static final Logger log = LoggerFactory.getLogger(HeatingManagerApplication.class);

    private static final int MS_TEN_UP = 1500;

    private static final int MS_TEN_DOWN = 1600;

    private static final Logger logger = LoggerFactory.getLogger(Heater.class);

    private GpioController controller;
    private GpioPinDigitalOutput gpioUp;
    private GpioPinDigitalOutput gpioDown;
    private GpioPinDigitalOutput gpioOnOff;


    public Heater(Pin pinOnOff, Pin pinUp, Pin pinDown) {

        String osName = System.getProperty("os.name");
        log.info("Detected " + osName);
        if (osName.startsWith("Windows")) {
            log.info("Using simulated GPIO provider");
            GpioFactory.setDefaultProvider(new SimulatedGpioProvider());
        } else {
            log.info("Using Rasberry PI GPIO provider with Broadcom Pin Numbering");
            GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
        }

        controller = GpioFactory.getInstance();

        gpioOnOff = controller.provisionDigitalOutputPin(pinOnOff, PinState.HIGH);
        gpioUp = controller.provisionDigitalOutputPin(pinUp, PinState.HIGH);
        gpioDown = controller.provisionDigitalOutputPin(pinDown, PinState.HIGH);

        gpioUp.setShutdownOptions(true);
        gpioDown.setShutdownOptions(true);
        gpioOnOff.setShutdownOptions(true);
    }

    public void up() {
        push(gpioUp, MS_TEN_UP);
    }

    public void down() {
        push(gpioDown, MS_TEN_DOWN);
    }

    public void resetToZero() {
        push(gpioDown, 11 * MS_TEN_DOWN); // 10 should be enough, +1 for extra buffering
    }

    private static void push(GpioPinDigitalOutput gpio, long time) {
        gpio.low();
        silentSleep(time);
        gpio.high();
        silentSleep(500);
    }

    private static void silentSleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            logger.warn("Interrupted sleep");
        }
    }
}
