/*
 * Copyright Cypress Semiconductor Corporation, 2014-2018 All rights reserved.
 *
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign),
 * United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate
 * license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 *
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 *
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 *
 *
 */
package com.cypress.cysmart.RDKEmulatorView;


/**
 * Attributes class for RDK emulator view
 */
class RDKAttributes {

    //Macro Definition
    public static final int AUDIO_SAMPLE_MAX = 32767;
    public static final int AUDIO_SAMPLE_MIN = -32768;
    public static final int ADPCM_INDEX_MAX = 88;
    public static final int LOWER_NIBBLE_MASK = 0xF;
    public static final int LOWER_BYTE_MASK = 0xFF;

    /*AUDIO packet(83 bytes) received contains 2 bytes of raw sample, 1 byte containing the index
  *into quantizer look up table and 80 bytes of adpcm codes from the 4th to 83rd byte. Hence the
  *actual adpcm code starts from 4th byte.*/
    public static final int ADPCM_CODE_START_INDEX = 3;

    /*Audio packet's first byte is lookup table index*/
    public static final int ADPCM_PREV_INDEX = 0;

    /*Audio packet's 2nd and 3rd byte contains 16 bit audio sample.*/
    public static final int ADPCM_PREV_SAMPLE_BYTE_0 = 1;
    public static final int ADPCM_PREV_SAMPLE_BYTE_1 = 2;
    public static final int BIT_1_POSITION = 1;
    public static final int BIT_1_MASK = 1 << BIT_1_POSITION;
    public static final int BIT_2_POSITION = 2;
    public static final int BIT_2_MASK = 1 << BIT_2_POSITION;
    public static final int BIT_3_POSITION = 3;
    public static final int BIT_3_MASK = 1 << BIT_3_POSITION;
    public static final int BIT_4_POSITION = 4;
    public static final int BIT_4_MASK = 1 << BIT_4_POSITION;
    public static final int BIT_8_POSITION = 8;
    public static final int BIT_8_MASK = 1 << BIT_8_POSITION;
    /* Bit position macros */
    private static final int BIT_0_POSITION = 0;
    /* Bit mask macros */
    public static final int BIT_0_MASK = 1 << BIT_0_POSITION;
    private static final int BIT_5_POSITION = 5;
    public static final int BIT_5_MASK = 1 << BIT_5_POSITION;
    private static final int BIT_6_POSITION = 6;
    public static final int BIT_6_MASK = 1 << BIT_6_POSITION;
    private static final int BIT_7_POSITION = 7;
    public static final int BIT_7_MASK = 1 << BIT_7_POSITION;

}
