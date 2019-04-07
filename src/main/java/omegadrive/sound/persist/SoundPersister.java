/*
 * SoundPersister
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

package omegadrive.sound.persist;

public interface SoundPersister {

    enum SoundType {
        PSG,
        FM,
        BOTH;
    }

    void persistSound(SoundType type, byte[] output);

    boolean isRecording();

    void stopRecording();

    void startRecording(SoundType soundType);

    default void persistPsg(byte[] output) {
        persistSound(SoundType.PSG, output);
    }

    default void persistFm(byte[] output) {
        persistSound(SoundType.FM, output);
    }

    default void persistMix(byte[] output) {
        persistSound(SoundType.BOTH, output);
    }
}
