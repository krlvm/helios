package omegadrive.cart.mapper.md;

import omegadrive.cart.loader.MdRomDbModel;
import omegadrive.util.LogHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * I2cEeprom
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 * <p>
 * From GenesisGxPlus:
 * https://github.com/ekeeke/Genesis-Plus-GX/blob/master/core/cart_hw/eeprom_i2c.c
 * <p>
 * TODO only supports 24C01 7 bits
 */
public class I2cEeprom {

    public static final I2cEeprom NO_OP = new I2cEeprom() {
        @Override
        public int eeprom_i2c_out() {
            return 0;
        }

        @Override
        public void eeprom_i2c_in(int data) {
        }
    };
    private final static Logger LOG = LogManager.getLogger(I2cEeprom.class.getSimpleName());
    private static boolean verbose = false;
    private int scl, prevScl;
    private int sda, prevSda;
    private int cycles, rw;
    private int buffer;
    private int address = 0;
    private int[] sram = new int[0];
    private int sizeMask = 0;
    private EepromState state = EepromState.STANDBY;

    public static I2cEeprom createInstance(MdRomDbModel.Entry entry) {
        I2cEeprom e = NO_OP;
        if (entry.hasEeprom()) {
            e = new I2cEeprom();
            MdRomDbModel.EEPROM eeprom = entry.getEeprom();
            e.sram = new int[eeprom.getSize()];
            e.sizeMask = e.sram.length - 1;
            LOG.info("Init " + eeprom);
        }
        return e;
    }

    public static void main(String[] args) {
        I2cEeprom i2c = new I2cEeprom();
        int[] writes = {3, 2, 0, 0, 0,
                2, 0, 0, 2, 0, 0, 2, 0, 0, 2, 0, 0, 2, 0, 0, 2, 0, 0,
                2, 0, 1, 3, 1, 1, 3, -1,
                2, 2, 3};
        for (int i = 0; i < writes.length; i++) {
            int val = writes[i];
            if (val < 0) {
                i2c.eeprom_i2c_out();
                break;
            }
            i2c.eeprom_i2c_in(val);
        }
    }

    public void eeprom_i2c_in(int data) {
        scl = (data & 2) >> 1;
        sda = data & 1;
        LogHelper.printLevel(LOG, Level.INFO, "SCL {}, SDA {}", scl, sda, verbose);
        if (state == EepromState.STANDBY) {
            checkStart();
        } else if (state == EepromState.GET_WORD_ADDR) {
            getWordAddr();
        } else if (state == EepromState.WRITE) {
            writeData();
        } else if (state == EepromState.READ) {
            readData();
        }
        prevScl = scl;
        prevSda = sda;
    }

    public int eeprom_i2c_out() {
        /* check EEPROM state */
        if (state == EepromState.READ) {
            /* READ cycle */
            if (cycles < 9) {
                /* return memory array (max 64kB) DATA bits */
                int index = address & 0xffff;
                int res = ((sram[index] >> (8 - cycles)) & 1);
                LogHelper.printLevel(LOG, Level.INFO, "{}, read {}, on cycle {}", state, res, cycles, verbose);
                return res;
            }
        } else if (cycles == 9) {
            /* ACK cycle */
            LogHelper.printLevel(LOG, Level.INFO, "{}, ack on cycle {}", state, cycles, verbose);
            return 0;
        }

        /* return latched /SDA input by default */
        return sda;
    }

    private void writeData() {
        checkStart();
        checkStop();
        if (prevScl > scl) {
            if (cycles < 9) {
                cycles++;
            } else {
                cycles = 1;
            }
        } else if (scl > prevScl) {
            if (cycles < 9) {
                /* latch DATA bits 7-0 to write buffer */
                buffer |= (sda << (8 - cycles));
                LogHelper.printLevel(LOG, Level.INFO, "{}, buffer {}", state, buffer, verbose);
            } else {
                LogHelper.printLevel(LOG, Level.INFO, "{}, val {}", state, buffer, verbose);
            }
        }
    }

    private void readData() {
        checkStart();
        checkStop();
        /* look for SCL HIGH to LOW transition */
        if (prevScl > scl) {
            if (cycles < 9) {
                cycles++;
            } else {
                cycles = 1;
            }
            /* look for SCL LOW to HIGH transition */
        } else if (prevScl < scl) {
            if (cycles == 9) {
                if (sda > 0) {
                    state = EepromState.WAIT_STOP;
                    LogHelper.printLevel(LOG, Level.INFO, "{}", state, verbose);
                } else {
                    /* increment Word Address (roll up at maximum array size) */
                    address = (address + 1) & sizeMask;
                }
            }
        }
    }

    private void getWordAddr() {
        checkStart();
        checkStop();
        /* look for SCL HIGH to LOW transition */
        if (prevScl > scl) {
            if (cycles < 9) {
                cycles++;
            } else {
                cycles = 1;
                state = rw > 0 ? EepromState.READ : EepromState.WRITE;
                buffer = 0;
                LogHelper.printLevel(LOG, Level.INFO, "{}", state, verbose);
            }
            /* look for SCL LOW to HIGH transition */
        } else if (prevScl < scl) {
            if (cycles < 8) {
                /* latch Word Address bits 6-0 */
                address |= (sda << (7 - cycles));
                LogHelper.printLevel(LOG, Level.INFO, "{}, address: {}, cycles: {}", state, address, cycles, verbose);
            } else if (cycles == 8) {
                /* latch R/W bit */
                rw = sda;
                LogHelper.printLevel(LOG, Level.INFO, "{}, rw latch: {}", state, rw, verbose);
            }
        }
    }

    // Check for START.
    private void checkStart() {
        if ((prevScl & scl) > 0 && (prevSda > sda)) {
            state = EepromState.GET_WORD_ADDR;
            cycles = 0;
            LogHelper.printLevel(LOG, Level.INFO, "{}", state, verbose);
        }
    }

    private void checkStop() {
        /* detect SDA LOW to HIGH transition while SCL is held HIGH */
        if (((prevScl & scl) > 0) && (sda > prevSda)) {
            state = EepromState.STANDBY;
            LogHelper.printLevel(LOG, Level.INFO, "{}", state, verbose);
        }
    }

    enum EepromState {STANDBY, GET_WORD_ADDR, WRITE, READ, WAIT_STOP}
}


