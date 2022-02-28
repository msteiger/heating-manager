package sma.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

/**
 * TODO: describe
 */
public class Heater {

    private static final int MS_TEN_UP = 1500;

    private static final int MS_TEN_DOWN = 1600;

    private static final Logger logger = LoggerFactory.getLogger(Heater.class);

    private GpioPinDigitalOutput gpioUp;
    private GpioPinDigitalOutput gpioDown;
    private GpioPinDigitalOutput gpioOnOff;


    public Heater(GpioController controller, Pin pinOnOff, Pin pinUp, Pin pinDown) {

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
