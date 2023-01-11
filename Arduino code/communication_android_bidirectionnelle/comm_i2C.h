#ifndef comm_i2C_H

#define comm_i2C_H

#include <Wire.h>

void envoyer_arduino_micro(char message[]){
  Wire.beginTransmission(8); // transmit to device #8
  Wire.write(message);        // sends the message.
  Wire.endTransmission();    // stop transmitting
}

#endif