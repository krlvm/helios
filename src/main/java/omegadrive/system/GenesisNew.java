/*
 * GenesisNew
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 11/10/19 15:05
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package omegadrive.system;

import omegadrive.SystemLoader;
import omegadrive.bus.DeviceAwareBus;
import omegadrive.bus.gen.GenesisBus;
import omegadrive.bus.gen.GenesisBusProvider;
import omegadrive.input.InputProvider;
import omegadrive.joypad.GenesisJoypad;
import omegadrive.m68k.M68kProvider;
import omegadrive.m68k.MC68000Wrapper;
import omegadrive.memory.IMemoryProvider;
import omegadrive.memory.MemoryProvider;
import omegadrive.savestate.BaseStateHandler;
import omegadrive.savestate.GenesisStateHandler;
import omegadrive.savestate.GstStateHandler;
import omegadrive.sound.SoundProvider;
import omegadrive.sound.javasound.JavaSoundManager;
import omegadrive.ui.DisplayWindow;
import omegadrive.util.RegionDetector;
import omegadrive.util.Util;
import omegadrive.vdp.model.BaseVdpProvider;
import omegadrive.vdp.model.GenesisVdpProvider;
import omegadrive.z80.Z80CoreWrapper;
import omegadrive.z80.Z80Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * Genesis emulator main class
 * <p>
 * MEMORY MAP:	https://en.wikibooks.org/wiki/Genesis_Programming
 */
public class GenesisNew extends BaseSystem<GenesisBusProvider, GenesisStateHandler> {

    public static boolean verbose = false;
    //NTSC_MCLOCK_MHZ = 53693175;
    //PAL_MCLOCK_MHZ = 53203424;
    //the emulation runs at MCLOCK_MHZ/MCLK_DIVIDER
    protected static int MCLK_DIVIDER = 7;
    protected static double VDP_RATIO = 4.0 / MCLK_DIVIDER;  //16 -> MCLK/4, 20 -> MCLK/5
    final static double[] vdpVals = {VDP_RATIO * BaseVdpProvider.MCLK_DIVIDER_FAST_VDP, VDP_RATIO * BaseVdpProvider.MCLK_DIVIDER_SLOW_VDP};
    protected static int M68K_DIVIDER = 7 / MCLK_DIVIDER;
    protected static int Z80_DIVIDER = 14 / MCLK_DIVIDER;
    protected static int FM_DIVIDER = 42 / MCLK_DIVIDER;
    private static Logger LOG = LogManager.getLogger(GenesisNew.class.getSimpleName());
    protected Z80Provider z80;
    protected M68kProvider cpu;
    protected double nextVdpCycle = vdpVals[0];
    protected int counter = 1;
    protected int elapsedNs;
    int next68kCycle = M68K_DIVIDER;
    int nextZ80Cycle = Z80_DIVIDER;
    long startCycle = System.nanoTime();
    Thread thread = Thread.currentThread();
    //fm emulation
    private double microsPerTick = 1;

    protected GenesisNew(DisplayWindow emuFrame) {
        super(emuFrame);
    }

    public static SystemProvider createNewInstance(DisplayWindow emuFrame) {
        return new GenesisNew(emuFrame);
    }

    @Override
    public void init() {
        stateHandler = GenesisStateHandler.EMPTY_STATE;
        joypad = new GenesisJoypad();
        inputProvider = InputProvider.createInstance(joypad);

        memory = MemoryProvider.createGenesisInstance();
        bus = new GenesisBus();
        vdp = GenesisVdpProvider.createVdp(bus);
        cpu = new MC68000Wrapper(bus);
        z80 = Z80CoreWrapper.createGenesisInstance(bus);
        //sound attached later
        sound = SoundProvider.NO_SOUND;

        bus.attachDevice(this).attachDevice(memory).attachDevice(joypad).attachDevice(vdp).
                attachDevice(cpu).attachDevice(z80);
        vdp.addVdpEventListener(((DeviceAwareBus) bus));
        reloadKeyListeners();
        createAndAddVdpEventListener();
    }

    protected void loop() {
        LOG.info("Starting game loop");
        updateVideoMode(true);

        do {
            try {
                run68k(counter);
                runZ80(counter);
                runFM(counter);
                runVdp(counter);
                counter++;
            } catch (Exception e) {
                LOG.error("Error main cycle", e);
                break;
            }
        } while (!runningRomFuture.isDone());
        LOG.info("Exiting rom thread loop");
    }

    @Override
    protected void newFrame() {
        copyScreenData(vdp.getScreenData());
        renderScreenInternal(getStats(startCycle));
        handleVdpDumpScreenData();
        updateVideoMode(false);
        elapsedNs = (int) (syncCycle(startCycle) - startCycle);
        sound.output(elapsedNs);
        if (thread.isInterrupted()) {
            LOG.info("Game thread stopped");
            runningRomFuture.cancel(true);
        }
        processSaveState();
        pauseAndWait();
        resetCycleCounters(counter);
        counter = 0;
        startCycle = System.nanoTime();
    }


    protected void runVdp(long counter) {
        if (counter >= nextVdpCycle) {
            int vdpMclk = vdp.run(1);
            nextVdpCycle += vdpVals[vdpMclk - 4];
        }
    }

    //TODO fifo full shoud not stop 68k
    //TODO fifo full and 68k uses vdp -> stop 68k
    //TODO check: interrupt shouldnt be processed when 68k is frozen but are
    //TODO prcessed when 68k is stopped
    protected void run68k(long counter) {
        if (counter == next68kCycle) {
            boolean isFrozen = bus.shouldStop68k();
            boolean canRun = !cpu.isStopped() && !isFrozen;
            int cycleDelay = 1;
            if (canRun) {
                cycleDelay = cpu.runInstruction();
            }
            //interrupts are processed after the current instruction
            if (!isFrozen) {
                bus.handleVdpInterrupts68k();
            }
            cycleDelay = Math.max(1, cycleDelay);
            next68kCycle += M68K_DIVIDER * cycleDelay;
        }
    }

    protected void runZ80(long counter) {
        if (counter == nextZ80Cycle) {
            int cycleDelay = 0;
            boolean running = bus.isZ80Running();
            if (running) {
                cycleDelay = z80.executeInstruction();
                bus.handleVdpInterruptsZ80();
            }
            cycleDelay = Math.max(1, cycleDelay);
            nextZ80Cycle += Z80_DIVIDER * cycleDelay;
        }
    }

    protected void runFM(int counter) {
        if (counter % FM_DIVIDER == 0) {
            bus.getFm().tick(microsPerTick);
        }
    }

    protected void updateVideoMode(boolean force) {
        if (force || videoMode != vdp.getVideoMode()) {
            videoMode = vdp.getVideoMode();
            microsPerTick = getMicrosPerTick();
            targetNs = (long) (region.getFrameIntervalMs() * Util.MILLI_IN_NS);
            LOG.info("Video mode changed: {}, microsPerTick: {}", videoMode, microsPerTick);
        }
    }

    private double getMicrosPerTick() {
        double mclkhz = videoMode.isPal() ? Util.GEN_PAL_MCLOCK_MHZ : Util.GEN_NTSC_MCLOCK_MHZ;
        return 1_000_000.0 / (mclkhz / (FM_DIVIDER * MCLK_DIVIDER));
    }

    @Override
    protected GenesisStateHandler createStateHandler(Path file, BaseStateHandler.Type type) {
        String fileName = file.toAbsolutePath().toString();
        return type == BaseStateHandler.Type.LOAD ?
                GstStateHandler.createLoadInstance(fileName) :
                GstStateHandler.createSaveInstance(fileName);
    }

    @Override
    protected void processSaveState() {
        if (saveStateFlag) {
            stateHandler.processState(vdp, z80, bus, sound, cpu, memory);
            if (stateHandler.getType() == GenesisStateHandler.Type.SAVE) {
                stateHandler.storeData();
            } else {
                sound.getPsg().reset();
            }
            stateHandler = GenesisStateHandler.EMPTY_STATE;
            saveStateFlag = false;
        }
    }

    @Override
    protected RegionDetector.Region getRegionInternal(IMemoryProvider memory, String regionOvr) {
        RegionDetector.Region romRegion = RegionDetector.detectRegion(memory);
        RegionDetector.Region ovrRegion = RegionDetector.getRegion(regionOvr);
        if (ovrRegion != null && ovrRegion != romRegion) {
            LOG.info("Setting region override from: " + romRegion + " to " + ovrRegion);
            romRegion = ovrRegion;
        }
        return romRegion;
    }

    protected void resetCycleCounters(int counter) {
        nextZ80Cycle -= counter;
        next68kCycle -= counter;
        nextVdpCycle -= counter;
    }

    protected void initAfterRomLoad() {
        sound = JavaSoundManager.createSoundProvider(getSystemType(), region);
        bus.attachDevice(sound);
        resetAfterRomLoad();
    }

    protected void resetAfterRomLoad() {
        vdp.setRegion(region);
        //detect ROM first
        joypad.init();
        vdp.init();
        bus.init();
        cpu.reset();
        z80.reset(); //TODO confirm this is needed
    }

    @Override
    public SystemLoader.SystemType getSystemType() {
        return SystemLoader.SystemType.GENESIS;
    }
}