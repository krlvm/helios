package omegadrive.joypad;

//	http://md.squee.co/315-5309
//	http://md.squee.co/Howto:Read_Control_Pads

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static omegadrive.joypad.JoypadProvider.JoypadAction.RELEASED;
import static omegadrive.joypad.JoypadProvider.JoypadButton.*;

/**
 * GenesisJoypad
 *
 * @author Federico Berti
 * <p>
 * <p>
 * 6-button controller steps
 * Bit 5	Bit 4	Bit 3	Bit 2	Bit 1	Bit 0
 * 1st step (write $40)	    C	B	    Right	Left	Down	Up
 * 2nd step (write $00)	Start	A	    0	    0	    Down	Up
 * 3rd step (write $40)	    C	B	    Right	Left	Down	Up
 * 4th step (write $00)	Start	A	    0	    0	    Down	Up
 * 5th step (write $40)	    C	B	    Right	Left	Down	Up
 * 6th step (write $00)	Start	A	    0	    0	    0	    0
 * 7th step (write $40)	    C	B	    Mode	X	    Y	    Z
 * <p>
 * https://www.plutiedev.com/controllers
 */
public class GenesisJoypad implements JoypadProvider {

    private static Logger LOG = LogManager.getLogger(GenesisJoypad.class.getSimpleName());

    //SGDK needs 0 here, otherwise it is considered a RESET
    long control1 = 0;
    long control2 = 0;
    long control3 = 0;

    int readStep1 = 0;
    int readStep2 = 0;

    private Map<JoypadButton, JoypadAction> stateMap1 = Maps.newHashMap(ImmutableMap.<JoypadButton, JoypadAction>builder().
            put(D, RELEASED).put(U, RELEASED).
            put(L, RELEASED).put(R, RELEASED).
            put(S, RELEASED).put(A, RELEASED).
            put(B, RELEASED).put(C, RELEASED).
            put(M, RELEASED).put(X, RELEASED).
            put(Y, RELEASED).put(Z, RELEASED).build());

    private Map<JoypadButton, JoypadAction> stateMap2 = Maps.newHashMap(stateMap1);

    boolean asserted1;
    boolean asserted2;

    public void initialize() {
        writeDataRegister1(0x40);
        writeDataRegister2(0x40);
    }

    public void writeDataRegister1(long data) {
        asserted1 = (data & 0x40) == 0;
    }

    public int readDataRegister1() {
        readStep1 = (readStep1 + 1) % 7;
        return readDataRegister(JoypadNumber.P1, asserted1, readStep1);
    }

    public int readDataRegister2() {
        readStep2 = (readStep2 + 1) % 7;
        return readDataRegister(JoypadNumber.P2, asserted2, readStep2);
    }

    private int readDataRegister(JoypadNumber n, boolean asserted, int readStep) {
        int res;
        if (asserted) {
            if (readStep != 5) {
                res = (getValue(n, S) << 5) | (getValue(n, A) << 4);    //	 (00SA0000)
            } else {
                //6 buttons
                res = (getValue(n, S) << 5) | (getValue(n, A) << 4) | (getValue(n, D) << 1) | (getValue(n, U));    //	 (00SA00DU)
            }
        } else {
            if (readStep != 6) {
                res = 0xC0 | (getValue(n, C) << 5) | (getValue(n, B) << 4) | (getValue(n, R) << 3) |
                        (getValue(n, L) << 2) | (getValue(n, D) << 1) | (getValue(n, U));    //	 (11CBRLDU)
            } else {
                //6 buttons
                res = 0xC0 | (getValue(n, C) << 5) | (getValue(n, B) << 4) | (getValue(n, M) << 3) |
                        (getValue(n, X) << 2) | (getValue(n, Y) << 1) | (getValue(n, Z));    //	 (11CBMXYZ)
            }
        }
        return res;
    }

    public void writeDataRegister2(long data) {
        asserted2 = (data & 0x40) == 0;
    }

    public int readDataRegister3() {
        return 0x3F;
    }

    private void writeControlCheck(int port, long data) {
        if (data != 0x40 && data != 0) {
            LOG.info("Setting ctrlPort{} to {}", port, Long.toHexString(data));
        }
    }

    public void writeControlRegister1(long data) {
        writeControlCheck(1, data);
        control1 = data;
    }

    public void writeControlRegister2(long data) {
        writeControlCheck(2, data);
        control2 = data;
    }

    public void writeControlRegister3(long data) {
        writeControlCheck(3, data);
        control3 = data;
    }

    private int getValue(JoypadNumber number, JoypadButton button) {
        switch (number) {
            case P1:
                return stateMap1.get(button).ordinal();
            case P2:
                return stateMap2.get(button).ordinal();
        }
        return 0;
    }

    @Override
    public void setButtonAction(JoypadNumber number, JoypadButton button, JoypadAction action) {
        switch (number) {
            case P1:
                stateMap1.put(button, action);
                break;
            case P2:
                stateMap2.put(button, action);
                break;
        }
    }

    public long readControlRegister1() {
        return control1;
    }

    public long readControlRegister2() {
        return control2;
    }

    public long readControlRegister3() {
        return control3;
    }


}
