/*
 * GenesisVdpProvider
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 07/04/19 16:01
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

package omegadrive.vdp.model;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import omegadrive.bus.gen.GenesisBusProvider;
import omegadrive.vdp.gen.GenesisVdp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;
import java.util.Map;

public interface GenesisVdpProvider extends BaseVdpProvider {

    Logger LOG = LogManager.getLogger(GenesisVdpProvider.class.getSimpleName());

    enum VdpRamType {
        VRAM,
        CRAM,
        VSRAM;
    }

    //    Bits CD3-CD0
// 0000b : VRAM read
// 0001b : VRAM write
// 0011b : CRAM write
// 0100b : VSRAM read
// 0101b : VSRAM write
// 1000b : CRAM read
    enum VramMode {
        vramRead(0b0000, VdpRamType.VRAM),
        vramWrite(0b0001, VdpRamType.VRAM),
        cramWrite(0b0011, VdpRamType.CRAM),
        vsramRead(0b0100, VdpRamType.VSRAM),
        vsramWrite(0b0101, VdpRamType.VSRAM),
        cramRead(0b1000, VdpRamType.CRAM),
        vramRead_8bit(0b1100, VdpRamType.VRAM);

        private VdpRamType ramType;
        private int addressMode;

        VramMode(int addressMode, VdpRamType ramType) {
            this.ramType = ramType;
            this.addressMode = addressMode;
        }

        public static VramMode getVramMode(int addressMode, boolean verbose) {
            VramMode m = null;
            switch (addressMode) {
                case 0b0000:
                    m = VramMode.vramRead;
                    break;
                case 0b0001:
                    m = VramMode.vramWrite;
                    break;
                case 0b0011:
                    m = VramMode.cramWrite;
                    break;
                case 0b0100:
                    m = VramMode.vsramRead;
                    break;
                case 0b0101:
                    m = VramMode.vsramWrite;
                    break;
                case 0b1000:
                    m = VramMode.cramRead;
                    break;
                case 0b1100:
                    m = VramMode.vramRead_8bit;
                    break;
                default:
                    if (verbose) {
                        LOG.warn("Unexpected value: {}, vramMode is null", addressMode);
                    }
            }
            return m;
        }

        public static VramMode getVramMode(int addressMode) {
            return getVramMode(addressMode, false);
        }

        public int getAddressMode() {
            return addressMode;
        }

        public VdpRamType getRamType() {
            return ramType;
        }

        public boolean isWriteMode() {
            return this == vramWrite || this == vsramWrite || this == cramWrite;
        }
    }

    enum VdpRegisterName {
        MODE_1, // 0
        MODE_2, // 1
        PLANE_A_NAMETABLE, //2
        WINDOW_NAMETABLE, //3
        PLANE_B_NAMETABLE, //4
        SPRITE_TABLE_LOC, //5
        SPRITE_PATTERN_BASE_ADDR, //6
        BACKGROUND_COLOR, //7
        UNUSED1, //8
        UNUSED2, //9
        HCOUNTER_VALUE, //10 - 0xA
        MODE_3, // 11 - 0xB
        MODE_4, //12 - 0xC
        HORIZONTAL_SCROLL_DATA_LOC, //13 - 0xD
        NAMETABLE_PATTERN_BASE_ADDR, // 14 -0xE
        AUTO_INCREMENT, //15 - 0xF
        PLANE_SIZE, //16 - 0x10
        WINDOW_PLANE_HOR_POS, //17 - 0x11
        WINDOW_PLANE_VERT_POS, //18 - 0x12
        DMA_LENGTH_LOW, //19 - 0x13
        DMA_LENGTH_HIGH, //20 - 0x14
        DMA_SOURCE_LOW, //21 - 0x15
        DMA_SOURCE_MID, //22 - 0x16
        DMA_SOURCE_HIGH //23 - 0x17
        ;

        private static Map<Integer, VdpRegisterName> lookup = ImmutableBiMap.copyOf(
                Maps.toMap(EnumSet.allOf(VdpRegisterName.class), VdpRegisterName::ordinal)).inverse();

        public static VdpRegisterName getRegisterName(int index) {
            return lookup.get(index);
        }
    }

    enum VdpPortType {DATA, CONTROL}


    static GenesisVdpProvider createVdp(GenesisBusProvider bus) {
        return GenesisVdp.createInstance(bus);
    }

    //always a word
    int readDataPort();

    void writeDataPort(long data);

    int readControl();

    void writeControlPort(long data);

    int getVCounter();

    int getHCounter();

    int getAddressRegister();

    void setAddressRegister(int value);

    boolean isIe0();

    boolean isIe1();

    void setDmaFlag(int value);

    /**
     * State of the Vertical interrupt pending flag
     *
     * @return
     */
    boolean getVip();

    /**
     * Set the Vertical interrupt pending flag
     *
     * @return
     */
    void setVip(boolean value);

    /**
     * State of the Horizontal interrupt pending flag
     *
     * @return
     */
    boolean getHip();

    /**
     * Set the Horizontal interrupt pending flag
     *
     * @return
     */
    void setHip(boolean value);

    boolean isShadowHighlight();

    IVdpFifo getFifo();

    VramMode getVramMode();

    InterlaceMode getInterlaceMode();

    default int getRegisterData(VdpRegisterName registerName) {
        return getRegisterData(registerName.ordinal());
    }

    default void updateRegisterData(VdpRegisterName registerName, int data) {
        updateRegisterData(registerName.ordinal(), data);
    }

}
