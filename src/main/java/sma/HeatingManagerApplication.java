package sma;

import static com.pi4j.io.gpio.RaspiPin.GPIO_13;
import static com.pi4j.io.gpio.RaspiPin.GPIO_17;
import static com.pi4j.io.gpio.RaspiPin.GPIO_22;
import static com.pi4j.io.gpio.RaspiPin.GPIO_27;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sma.domain.em.DataBlock;
import sma.service.EnergyMeterService;
import sma.service.Heater;
import sma.service.HeaterMeterService;


public class HeatingManagerApplication {

    private static final Logger log = LoggerFactory.getLogger(HeatingManagerApplication.class);

    public static void main(String[] args) throws InterruptedException {

        EnergyMeterService meter = new EnergyMeterService();
        Heater heater = new Heater(GPIO_22, GPIO_27, GPIO_17);

        HeaterMeterService heaterMeter = new HeaterMeterService(GPIO_13);

        boolean logIdleDead = true;
        boolean logIdleMax = true;

        log.info("Reset to zero ..");
        heater.resetToZero();
        int estimatedValue = 0;
        log.info("Reset to zero .. complete");

        while (true) {
            try {
                DataBlock block = meter.waitForBroadcast();
                double surplus = Math.floor(block.getPowerOut() - block.getPowerIn()); // round to improve formatting
                if (surplus > 150.0) {
                    logIdleDead = true;
                    if (estimatedValue < 11) {
                        heater.up();
                        estimatedValue++;
                        logIdleMax = true;
                        log.info("Up to [{}] || Power: {} Watt", estimatedValue, surplus);
                    } else {
                        if (logIdleMax) log.info("Enter idle mode because maximum (100) is reached: {} Watt", surplus);
                        logIdleMax = false;
                    }
                } else if (surplus < 0) {
                    logIdleDead = true;
                    if (estimatedValue > -1) {
                        heater.down();
                        estimatedValue--;
                        logIdleMax = true;
                        log.info("Down to [{}] || Power: {} Watt", estimatedValue, surplus);
                    } else {
                        if (logIdleMax) log.info("Enter idle mode because minimum (0) is reached: {} Watt", surplus);
                        logIdleMax = false;
                    }
                } else {
                    if (logIdleDead) log.info("Enter idle mode due to dead zone: {} Watt", surplus);
                    logIdleDead = false;
                    logIdleMax = true;
                }
            } catch (IOException e) {
                log.error("Failed to connect: {}", e.toString());
            }

            Thread.sleep(10_000);
        }
    }
}

