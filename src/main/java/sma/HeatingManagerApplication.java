package sma;

import static com.pi4j.io.gpio.RaspiPin.GPIO_13;
import static com.pi4j.io.gpio.RaspiPin.GPIO_17;
import static com.pi4j.io.gpio.RaspiPin.GPIO_22;
import static com.pi4j.io.gpio.RaspiPin.GPIO_27;

import java.io.IOException;
import java.time.LocalTime;
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
import sma.service.TemperatureService;
import sma.service.WebServer;


public class HeatingManagerApplication {

    private static final Logger log = LoggerFactory.getLogger(HeatingManagerApplication.class);

    public static void main(String[] args) throws InterruptedException {

        EnergyMeterService meter = new EnergyMeterService();

        String tempRoot = isWindows() ? "./" : "/sys/bus/w1/";
        TemperatureService tempService = new TemperatureService("28-01205b7cac50", tempRoot);

        GpioController controller = createController();

        Heater heater = new Heater(controller, GPIO_22, GPIO_27, GPIO_17);

        HeaterMeterService heaterMeter = new HeaterMeterService(controller, GPIO_13);

        int estimatedValue = 0;

        int logPingTimer = 0;
        int temperatureTimer = 0;
        boolean temperatureStop = false;

        boolean inSleepMode = false;

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

        boolean logIdleMax = true;
        boolean logIdleMin = true;

        while (true) {
            try {
                LocalTime time = LocalTime.now();
                if (time.getHour() > 21 || time.getHour() < 6) {
                    if (!inSleepMode) {
                        log.info("Going to sleep ...");
                        heater.resetToZero();
                        estimatedValue = -1;
                        inSleepMode = true;
                    }
                    Thread.sleep(60_000);
                    continue;
                }

                if (inSleepMode) {
                    log.info("Time to wake up!");
                    inSleepMode = false;
                }

                // check every 10th iteration if temperature is still below maximum
                if (temperatureTimer++ % 10 == 0) {
                    float temperature = tempService.getTemperature();   // slow call, about ~2sec

                    if (!temperatureStop && temperature > 60) {
                        log.info("Max. temp reached - shutting down heater");
                        temperatureStop = true;
                        heater.resetToZero();
                        estimatedValue = -1;
                    }
                    if (temperatureStop && temperature < 55) {
                        log.info("Max. temp no longer reached - continuing ..");
                        temperatureStop = false;
                    }
                }

                if (temperatureStop) {
                    Thread.sleep(10_000);
                    continue;
                }

                DataBlock block = meter.waitForBroadcast();
                if (block.getSerialNumber() == -1) {
                    log.warn("Invalid data from energy meter - skipping");
                    continue;
                }
                double surplus = Math.floor(block.getPowerOut() - block.getPowerIn()); // round to improve formatting

                if (surplus > 100_000 || surplus < -100_000) {
                    log.warn("Invalid surplus value '{}'- skipping", surplus);
                    continue;
                }

                if (surplus > 150.0) {
                    if (estimatedValue < 11) {
                        heater.up();
                        estimatedValue++;
                        logIdleMax = true;
                        log.info("Up to [{}] || Power: {} Watt", estimatedValue * 10, surplus);
                    } else {
                        if (logIdleMax) log.info("Enter idle mode because maximum (110) is reached: {} Watt", surplus);
                        logIdleMax = false;
                    }
                } else if (surplus < 0) {
                    if (estimatedValue > -1) {
                        heater.down();
                        estimatedValue--;
                        logIdleMin = true;
                        log.info("Down to [{}] || Power: {} Watt", estimatedValue * 10, surplus);
                    } else {
                        if (logIdleMin) log.info("Enter idle mode because minimum (-10) is reached: {} Watt", surplus);
                        logIdleMin = false;
                    }
                }
            } catch (IOException e) {
                log.error("Failed to connect: {}", e.toString());
                heater.resetToZero();
                estimatedValue = -1;
            }

            // log something every now and then
            if (logPingTimer++ % 120 == 0) {
                log.info("Running at estimated level: {}", estimatedValue * 10);
            }

            Thread.sleep(5_000);
        }
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.startsWith("Windows");
    }

    private static GpioController createController() {
        if (isWindows()) {
            log.info("Using simulated GPIO provider");
            GpioFactory.setDefaultProvider(new SimulatedGpioProvider());
        } else {
            log.info("Using Rasberry PI GPIO provider with Broadcom Pin Numbering");
            GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
        }

        return GpioFactory.getInstance();
    }
}

