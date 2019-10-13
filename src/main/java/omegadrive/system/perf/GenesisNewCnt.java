/*
 * GenesisNewCnt
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

package omegadrive.system.perf;

import omegadrive.system.GenesisNew;
import omegadrive.ui.DisplayWindow;
import omegadrive.util.RegionDetector;
import omegadrive.util.Util;
import omegadrive.util.VideoMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.stream.IntStream;

/**
 * Genesis emulator main class
 * <p>
 * MEMORY MAP:	https://en.wikibooks.org/wiki/Genesis_Programming
 */
public class GenesisNewCnt extends GenesisNew {

    static int mclkHz, m68kRef, vdpRef, z80Ref, fmRef;
    private static Logger LOG = LogManager.getLogger(GenesisNewCnt.class.getSimpleName());
    int[] cycleVdpFrame = new int[RegionDetector.Region.USA.getFps()];
    int[] cycle68kFrame = new int[RegionDetector.Region.USA.getFps()];
    int[] cycleZ80Frame = new int[RegionDetector.Region.USA.getFps()];
    int[] cycleFmFrame = new int[RegionDetector.Region.USA.getFps()];
    int cycleVdpCnt, cycle68kCnt, cycleZ80cnt, cycleFmCnt;
    long frameWaitNs, lastSecTimeNs;
    int totalCycles, frameCnt;

    protected GenesisNewCnt(DisplayWindow emuFrame) {
        super(emuFrame);
    }

    private void stats() {
        cycle68kFrame[frameCnt] = cycle68kCnt;
        cycleVdpFrame[frameCnt] = cycleVdpCnt;
        cycleZ80Frame[frameCnt] = cycleZ80cnt;
        cycleFmFrame[frameCnt] = cycleFmCnt;

        frameWaitNs += elapsedNs;
        frameCnt++;
        totalCycles += counter;

        if (frameCnt == videoMode.getRegion().getFps()) {
            long nowNs = System.nanoTime();
            long lastSecLenMs = Duration.ofNanos(nowNs - lastSecTimeNs).toMillis();
            double vdpAvg = IntStream.range(0, frameCnt).mapToDouble(i -> cycleVdpFrame[i]).sum();
            double m68kAvg = IntStream.range(0, frameCnt).mapToDouble(i -> cycle68kFrame[i]).sum();
            double z80Avg = IntStream.range(0, frameCnt).mapToDouble(i -> cycleZ80Frame[i]).sum();
            double fmAvg = IntStream.range(0, frameCnt).mapToDouble(i -> cycleFmFrame[i]).sum();
            long waitMs = 1000 - Duration.ofNanos(frameWaitNs).toMillis();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Last 1s duration in ms %d, errorPerc %f%n", lastSecLenMs, 100 - (100 * lastSecLenMs / 1000.0)));
            sb.append(String.format("helios cycles: %d, waitMs %d%n", totalCycles, waitMs));
            sb.append(String.format("68k cycles: %f, ref %d, errorPerc %f%n", m68kAvg, m68kRef, 100 - (100 * m68kRef / m68kAvg)));
            sb.append(String.format("Z80 cycles: %f, ref: %d, errorPerc: %f%n", z80Avg, z80Ref, 100 - (100 * z80Ref / z80Avg)));
            sb.append(String.format("FM cycles: %f, ref: %d, errorPerc: %f%n", fmAvg, fmRef, 100 - (100 * fmRef / fmAvg)));
            sb.append(String.format("VDP cycles: %f%n", vdpAvg));
//            sb.append(String.format("VDP cycles: %f, ref: %d, errorPerc: %f%n", vdpAvg,vdpRef, 100 - (100*vdpRef/vdpAvg)));

            LOG.info(sb.toString());
            frameCnt = 0;
            frameWaitNs = 0;
            totalCycles = 0;
            lastSecTimeNs = nowNs;
        }
    }

    @Override
    protected void updateVideoMode(boolean force) {
        VideoMode prev = videoMode;
        super.updateVideoMode(force);
        if (videoMode != prev || force) {
            mclkHz = videoMode.isPal() ? Util.GEN_PAL_MCLOCK_MHZ : Util.GEN_NTSC_MCLOCK_MHZ;
            m68kRef = mclkHz / (M68K_DIVIDER * MCLK_DIVIDER);
            z80Ref = mclkHz / (Z80_DIVIDER * MCLK_DIVIDER);
            fmRef = mclkHz / (FM_DIVIDER * MCLK_DIVIDER);
        }
    }

    @Override
    protected String getStats(long nowNs) {
        stats();
        return super.getStats(nowNs);
    }

    @Override
    protected void resetCycleCounters(int counter) {
        super.resetCycleCounters(counter);
        cycleVdpCnt = cycleFmCnt = cycle68kCnt = cycleZ80cnt = 0;
    }

    @Override
    protected void runVdp(long counter) {
        cycleVdpCnt += counter >= nextVdpCycle ? 1 : 0;
        super.runVdp(counter);
    }

    @Override
    protected void run68k(long counter) {
        super.run68k(counter);
        if (counter % M68K_DIVIDER == 0) {
            cycle68kCnt++;
        }
    }

    @Override
    protected void runZ80(long counter) {
        super.runZ80(counter);
        if (counter % Z80_DIVIDER == 0) {
            cycleZ80cnt++;
        }
    }

    @Override
    protected void runFM(int counter) {
        super.runFM(counter);
        if (counter % FM_DIVIDER == 0) {
            cycleFmCnt++;
        }
    }
}