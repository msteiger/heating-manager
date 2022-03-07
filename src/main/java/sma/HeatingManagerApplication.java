package sma;

import static com.pi4j.io.gpio.RaspiPin.GPIO_13;
import static com.pi4j.io.gpio.RaspiPin.GPIO_17;
import static com.pi4j.io.gpio.RaspiPin.GPIO_22;
import static com.pi4j.io.gpio.RaspiPin.GPIO_27;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.RaspiGpioProvider;
import com.pi4j.io.gpio.RaspiPinNumberingScheme;
import com.pi4j.io.gpio.SimulatedGpioProvider;

import sma.domain.em.DataBlock;
import sma.service.EnergyMeterService;
import sma.service.Heater;
import sma.service.HeaterMeterService;
import sma.service.WebServer;


public class HeatingManagerApplication {

    private static final Logger log = LoggerFactory.getLogger(HeatingManagerApplication.class);

    public static void main(String[] args) throws InterruptedException {

        EnergyMeterService meter = new EnergyMeterService();

        GpioController controller = createController();

        Heater heater = new Heater(controller, GPIO_22, GPIO_27, GPIO_17);

        HeaterMeterService heaterMeter = new HeaterMeterService(controller, GPIO_13);

        int estimatedValue = 0;

        Supplier<Map<String, Object>> dataProvider = () -> {
            return Map.<String, Object>of(
                    "pfcLevel", heaterMeter.getPfcLevel(),
                    "rawPfcLevel", heaterMeter.getRawPfcLevel(),
                    "maxPfcLevel", heaterMeter.getMaxPfcLevel(),
                    "currentPower", heaterMeter.getPower(),
                    "currentRawPower", heaterMeter.getRawPower(),
                    "startTime", heaterMeter.getStartTime(),
                    "totalEnergyWh", heaterMeter.getWattHours()
                    );
        };

        WebServer server = new WebServer(dataProvider);
        server.start();

        log.info("Reset to zero ..");
        heater.resetToZero();
        log.info("Reset to zero .. complete");

        boolean logIdleDead = true;
        boolean logIdleMax = true;

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
                        log.info("Up to [{}] || Power: {} Watt", estimatedValue * 10, surplus);
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
                        log.info("Down to [{}] || Power: {} Watt", estimatedValue * 10, surplus);
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

            Thread.sleep(5_000);
        }
    }

    private static GpioController createController() {
        String osName = System.getProperty("os.name");
        log.info("Detected " + osName);
        if (osName.startsWith("Windows")) {
            log.info("Using simulated GPIO provider");
            GpioFactory.setDefaultProvider(new SimulatedGpioProvider());
        } else {
            log.info("Using Rasberry PI GPIO provider with Broadcom Pin Numbering");
            GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
        }

        return GpioFactory.getInstance();
    }
}

