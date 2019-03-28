package omegadrive;

import omegadrive.bus.BaseBusProvider;
import omegadrive.bus.sg1k.ColecoBus;
import omegadrive.bus.sg1k.Sg1000BusProvider;
import omegadrive.input.InputProvider;
import omegadrive.joypad.ColecoPad;
import omegadrive.memory.IMemoryProvider;
import omegadrive.memory.MemoryProvider;
import omegadrive.sound.SoundProvider;
import omegadrive.sound.javasound.JavaSoundManager;
import omegadrive.util.FileLoader;
import omegadrive.util.RegionDetector;
import omegadrive.util.Util;
import omegadrive.util.VideoMode;
import omegadrive.vdp.Sg1000Vdp;
import omegadrive.z80.Z80CoreWrapper;
import omegadrive.z80.Z80Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.filechooser.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;

/**
 * Sg1000 emulator main class
 *
 * @author Federico Berti
 * <p>
 */
public class Coleco extends BaseSystem {

    private static Logger LOG = LogManager.getLogger(Coleco.class.getSimpleName());

    protected Z80Provider z80;
    private Sg1000BusProvider bus;

    public static boolean verbose = false;

    public static void main(String[] args) throws Exception {
        loadProperties();
        InputProvider.bootstrap();
        boolean isHeadless = isHeadless();
        LOG.info("Headless mode: " + isHeadless);
        Coleco coleco = new Coleco(isHeadless);
        if (args.length > 0) {
            //linux pulseaudio can crash if we start too quickly
            Util.sleep(250);
            String filePath = args[0];
            LOG.info("Launching file at: " + filePath);
            coleco.handleNewRom(Paths.get(filePath));
        }
        if (isHeadless) {
            Util.waitForever();
        }
    }

    public static SystemProvider createInstance() {
        return createInstance(false);
    }

    public static SystemProvider createInstance(boolean headless) {
        InputProvider.bootstrap();
        Coleco genesis = null;
        try {
            genesis = new Coleco(headless);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return genesis;
    }

    protected Coleco(boolean isHeadless) throws InvocationTargetException, InterruptedException {
        super(isHeadless);
    }

    @Override
    public void init() {
        joypad = new ColecoPad();
        inputProvider = InputProvider.createInstance(joypad);

        memory = MemoryProvider.createSg1000Instance();
        bus = new ColecoBus();
        vdp = new Sg1000Vdp();

        //z80, sound attached later
        sound = SoundProvider.NO_SOUND;
        bus.attachDevice(this).attachDevice(memory).attachDevice(joypad).attachDevice(vdp).attachDevice(sound).
                attachDevice(vdp);
        reloadKeyListeners();
    }

    @Override
    protected RegionDetector.Region getRegionInternal(IMemoryProvider memory, String regionOvr) {
        return RegionDetector.Region.JAPAN;
    }

    @Override
    protected FileFilter getRomFileFilter() {
        return FileLoader.ROM_FILTER;
    }

    private static int VDP_DIVIDER = 1;  //10.738635 Mhz
    private static int Z80_DIVIDER = 3; //3.579545 Mhz

    int nextZ80Cycle = Z80_DIVIDER;
    int nextVdpCycle = VDP_DIVIDER;

    @Override
    protected void loop() {
        LOG.info("Starting game loop");

        int counter = 1;
        long startCycle = System.nanoTime();
        targetNs = (long) (region.getFrameIntervalMs() * Util.MILLI_IN_NS);
        updateVideoMode();
        //TODO hack
        z80.unrequestBus();
        z80.disableReset();
        //TODO hack

        do {
            try {
                runZ80(counter);
                runVdp(counter);
                if (canRenderScreen) {
                    renderScreenInternal(getStats(System.nanoTime()));
                    handleVdpDumpScreenData();
                    updateVideoMode();
                    canRenderScreen = false;
                    int elapsedNs = (int) (syncCycle(startCycle) - startCycle);
                    if (Thread.currentThread().isInterrupted()) {
                        LOG.info("Game thread stopped");
                        break;
                    }
//                    processSaveState();
                    pauseAndWait();
                    resetCycleCounters(counter);
                    counter = 0;
                    bus.newFrame();
                    startCycle = System.nanoTime();
                }
                counter++;
            } catch (Exception e) {
                LOG.error("Error main cycle", e);
                break;
            }
        } while (!runningRomFuture.isDone());
        LOG.info("Exiting rom thread loop");
    }

    @Override
    protected void initAfterRomLoad() {
        sound = JavaSoundManager.createSoundProvider(region);
        z80 = Z80CoreWrapper.createSg1000Instance(bus);
        bus.attachDevice(sound).attachDevice(z80);

        resetAfterRomLoad();
    }

    private void resetAfterRomLoad() {
        //detect ROM first
        bus.reset();
        joypad.initialize();
        vdp.init();
        z80.reset();
        z80.initialize();
    }

    @Override
    protected void saveStateAction(String fileName, boolean load, int[] data) {
        LOG.error("Not implemented!");
    }

    @Override
    protected void processSaveState() {
        LOG.error("Not implemented!");
    }

    @Override
    protected BaseBusProvider getBusProvider() {
        return bus;
    }

    private void resetCycleCounters(int counter) {
        nextZ80Cycle -= counter;
        nextVdpCycle -= counter;
    }


    private void updateVideoMode() {
        VideoMode vm = vdp.getVideoMode();
        if (videoMode != vm) {
            LOG.info("Video mode changed: {}", vm);
            videoMode = vm;
        }
    }

    /**
     * NTSC, 256x192
     * -------------
     * <p>
     * Lines  Description
     * <p>
     * 192    Active display
     * 24     Bottom border
     * 3      Bottom blanking
     * 3      Vertical blanking
     * 13     Top blanking
     * 27     Top border
     * <p>
     * V counter values
     * 00-DA, D5-FF
     * <p>
     * vdpTicksPerFrame = (NTSC_SCANLINES = ) 262v * (H32_PIXELS =) 342 = 89604
     * vdpTicksPerSec = 5376240
     */


    private void runVdp(long counter) {
        if (counter % 2 == 1) {
            if (vdp.run(1)) {
                canRenderScreen = true;
                vdpScreen = vdp.getScreenData();
            }
        }
    }


    private void runZ80(long counter) {
        if (counter == nextZ80Cycle) {
            bus.handleVdpInterruptsZ80();
            int cycleDelay = z80.executeInstruction();
            cycleDelay = Math.max(1, cycleDelay);
            nextZ80Cycle += Z80_DIVIDER * cycleDelay;
        }
    }
}