package omegadrive.vdp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2018
 */
public class VdpDmaHandlerTest extends BaseVdpDmaHandlerTest {

    private static Logger LOG = LogManager.getLogger(VdpDmaHandlerTest.class.getSimpleName());

    @Test
    public void testDMACopy_inc0() {
        int[] expected = {0xF0, 0x44, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D};
        testDMACopyInternal(0, expected);
    }

    @Test
    public void testDMACopy_inc1() {
        int[] expected = {0x11, 0x22, 0xF0, 0x44, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D};
        testDMACopyInternal(1, expected);
    }

    @Test
    public void testDMACopy_inc2() {
        //$F022, $F011, $F044
        int[] expected = {0xF0, 0x22, 0xF0, 0x11, 0xF0, 0x44, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D};
        testDMACopyInternal(2, expected);
    }

    @Test
    public void testDMACopy_inc4() {
        //$F022, $F00D, $F011, $F00D, $F044,
        int[] expected = {0xF0, 0x22, 0xF0, 0x0D, 0xF0, 0x11, 0xF0, 0x0D, 0xF0, 0x44, 0xF0, 0x0D, 0xF0, 0x0D, 0xF0, 0x0D};
        testDMACopyInternal(4, expected);
    }

    /**
     * VDPFIFOTesting #42
     * <p>
     * ThunderForce IV breaks if this fails
     */
    @Test
    public void testDMA_Fill_VRAM_Even_inc0() {
        long dmaFillCommand = 0x40020082; //DMA fill at VRAM address 0x8002
        int[] expected = {0x2, 0xEE, 0x68, 0x68, 0x06, 0xEA};
        testDMAFillInternal(dmaFillCommand, 0, expected);
    }

    @Test
    public void testDMA_Fill_VRAM_inc1() {
        testDMA_Fill_VRAM_Even_inc1();
        testDMA_Fill_VRAM_Odd_inc1();
    }

    @Test
    public void testDMA_Fill_VRAM_Even_inc1() {
        long dmaFillCommand = 0x40020082; //DMA fill at VRAM address 0x8002
        int[] expected = {0x2, 0xEE, 0x68, 0xAC, 0x68, 0x68, 0x68, 0x68, 0x0A, 0xE6};
        testDMAFillInternal(dmaFillCommand, 1, expected);
    }

    @Test
    public void testDMA_Fill_VRAM_Even_inc2() {
        long dmaFillCommand = 0x40020082; //DMA fill at VRAM address 0x8002
        int[] expected = {0x2, 0xEE, 0x68, 0xAC, 0x06, 0x68, 0x08, 0x68,
                0x0A, 0x68, 0x0C, 0x68, 0x0E, 0x68, 0x02};
        testDMAFillInternal(dmaFillCommand, 2, expected);
    }

    @Test
    public void testDMA_Fill_VRAM_Even_inc4() {
        long dmaFillCommand = 0x40020082; //DMA fill at VRAM address 0x8002
        int[] expected = {0x2, 0xEE, 0x68, 0xAC, 0x06, 0xEA, 0x08, 0x68, 0x0A, 0xE6,
                0x0C, 0x68, 0x0E, 0xE2, 0x02, 0x68,
                0x04, 0xCE, 0x06, 0x68, 0x08, 0xCA, 0x0A, 0x68, 0x0C, 0xC6};
        testDMAFillInternal(dmaFillCommand, 4, expected);
    }

    @Test
    public void testDMA_Fill_VRAM_Odd_inc0() {
        long dmaFillCommand = 0x40030082; //DMA fill at VRAM address 0x8003
        int[] expected = {0x2, 0xEE, 0x68, 0x68, 0x06, 0xEA};
        testDMAFillInternal(dmaFillCommand, 0, expected);
    }

    @Test
    public void testDMA_Fill_VRAM_Odd_inc1() {
        long dmaFillCommand = 0x40030082; //DMA fill at VRAM address 0x8003
        //dc.w $02EE, $AC68, $6868, $6868, $0A68, $0CE4, $0EE2,
        int[] expected = {0x2, 0xEE, 0xAC, 0x68, 0x68, 0x68, 0x68, 0x68, 0x0A, 0x68, 0x0C, 0xE4};
        testDMAFillInternal(dmaFillCommand, 1, expected);
    }

    @Test
    public void testDMA_Fill_VRAM_Odd_inc2() {
        long dmaFillCommand = 0x40030082; //DMA fill at VRAM address 0x8003
        //dc.w $02EE, $AC68, $68EA, $68E8, $68E6, $68E4, $68E2, $02E0
        int[] expected = {0x2, 0xEE, 0xAC, 0x68, 0x68, 0xEA, 0x68, 0xE8,
                0x68, 0xE6, 0x068, 0xE4, 0x068, 0xE2, 0x02, 0xE0};
        testDMAFillInternal(dmaFillCommand, 2, expected);
    }

    @Test
    public void testDMA_Fill_VRAM_Odd_inc4() {
        long dmaFillCommand = 0x40030082; //DMA fill at VRAM address 0x8003
        //dc.w $02EE, $AC68, $06EA, $68E8, $0AE6, $68E4, $0EE2, $68E0, $04CE, $68CC, $08CA, $68C8, $0CC6, $0EC4, $02C2, $04C0
        int[] expected = {0x2, 0xEE, 0xAC, 0x68, 0x06, 0xEA, 0x68, 0xE8, 0x0A, 0xE6,
                0x68, 0xE4, 0x0E, 0xE2, 0x68, 0xE0,
                0x04, 0xCE, 0x68, 0xCC, 0x08, 0xCA, 0x68, 0xC8, 0x0C, 0xC6};
        testDMAFillInternal(dmaFillCommand, 4, expected);
    }
}