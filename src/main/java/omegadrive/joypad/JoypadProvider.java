/*
 * JoypadProvider
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

package omegadrive.joypad;

import omegadrive.Device;

import static omegadrive.joypad.JoypadProvider.JoypadButton.*;

public interface JoypadProvider extends Device {

    JoypadButton[] directionButton = {D, L, R, U};

    enum JoypadAction {
        PRESSED,
        RELEASED
    }

    enum JoypadNumber {
        P1, P2
    }

    enum JoypadType {
        BUTTON_2,
        BUTTON_3,
        BUTTON_6
    }

    enum JoypadButton {
        A, B, C, X, Y, Z, M, S, U, D, L, R,
        K0, K1, K2, K3, K4, K5, K6, K7, K8, K9, K_AST, K_HASH
    }

    int readDataRegister1();

    int readDataRegister2();

    int readDataRegister3();

    long readControlRegister1();

    long readControlRegister2();

    long readControlRegister3();

    void writeDataRegister1(long data);

    void writeDataRegister2(long data);

    void writeControlRegister1(long data);

    void writeControlRegister2(long data);

    void writeControlRegister3(long data);

    void setButtonAction(JoypadNumber number, JoypadButton button, JoypadAction action);

    boolean hasDirectionPressed(JoypadNumber number);

    String getState(JoypadNumber number);

    void newFrame();
}
