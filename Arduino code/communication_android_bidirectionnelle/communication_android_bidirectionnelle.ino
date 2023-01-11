#include <Servo.h>
#include <Wire.h> //TODO : à enlever après. C'est uniquement pour les essais.
#include "comm_i2C.h"
//inspiré de http://android-er.blogspot.com/2014/09/bi-directional-communication-between_24.html

int pinLED = 13;

const int ST_0 = 0;      //waiting Sync word
const int ST_1_CMD = 1;  //Waiting CMD
const int ST_2_LENGTH= 2;//Receiving Length for CMD_03_TEXT
const int ST_3_DATA= 3;  //Receiving Data for CMD_03_TEXT
const byte IGNORE_00 = 0x00;
const byte SYNC_WORD = 0xFF;
const byte CMD_01_LEDON = 0x01;
const byte CMD_02_LEDOFF= 0x02;
const byte CMD_03_TEXT  = 0x03;
int cmdState;
int dataIndex;

//longueur maximale des messages reçus (en nombre de caractères)
const int MAX_LENGTH = 16;
uint8_t data[MAX_LENGTH];
int dataLength;
const uint8_t servoPins[4] = {3, 5, 6, 9};  //Dans l'ordre : aileron gauche, aileron droit, gouverne de profondeur, autre.

//Servo servo1;
//Servo servo2;
//Servo servo3;
Servo servos[4];// = {servo1, servo2, servo3};
const uint8_t posMinServos[4] = {70 , 70 , 70 , 70 };  //Angles minimum de chacun des 4 servos. (entre 0 et 180)
const uint8_t posMaxServos[4] = {150, 150, 150, 150};  //Angles maximum de chacun des 4 servos. (entre 0 et 180)


void setup() {
  Wire.begin(); //TODO : à enlever après. C'est uniquement pour les essais.
  envoyer_arduino_micro("Bonjour !");
  Serial.begin(9600);
  for(int i=0; i<4; i++){
    pinMode(servoPins[i], OUTPUT);
    servos[i].attach(servoPins[i]);
    /*for(uint8_t j = posMinServos[i]; j < posMaxServos[i]; j+=5){
      servos[i].write(j);
      Serial.print("Servo "); Serial.print(i); Serial.print(" pos : "); Serial.println(j);
      delay(30);
    }*/
    
  }
}

uint32_t heure_dernier_message = millis();
const uint16_t timeout_message = 1000;

void loop() {
  delay(100);
  if(Serial.available()){
    byte nb_char_available = Serial.available();
    heure_dernier_message = millis();

    //Un message complet contient 7 bytes : le caractère de synchronisation ()
    while((Serial.available() < 7) && (millis() - heure_dernier_message < timeout_message)){
      delay(30);
    }
    heure_dernier_message = millis();
    
    while(Serial.available() > 0){
      int byteIn = Serial.read();
      cmdHandle(byteIn);
    }
  }
  //si le tableau data contient autre chose que des zéros, c'est qu'il y a un ordre à exécuter.
  if(data[0] != 0){
    envoyer_arduino_micro("reçu : ");
    char x[4];
    itoa(data[0], x, DEC);
    envoyer_arduino_micro(x); envoyer_arduino_micro(" | ");
    itoa(data[1], x, DEC);
    envoyer_arduino_micro(x); envoyer_arduino_micro(" | ");
    itoa(data[2], x, DEC);
    envoyer_arduino_micro(x); envoyer_arduino_micro(" | ");
    itoa(data[3], x, DEC);
    envoyer_arduino_micro(x); envoyer_arduino_micro(" | ");
    
    int commandesServos[4];
    for(int i=0; i<4; i++){
      commandesServos[i] = map(data[i], 0, 254, posMinServos[i], posMaxServos[i]);     // scale it for use with the servo (value between 0 and 180)
      servos[i].write(commandesServos[i]);
      data[i] = 0;
    }
    envoyer_arduino_micro("J'envoie : ");
    itoa(commandesServos[0], x, DEC);
    envoyer_arduino_micro(x); envoyer_arduino_micro(" | ");
    itoa(commandesServos[1], x, DEC);
    envoyer_arduino_micro(x); envoyer_arduino_micro(" | ");
    itoa(commandesServos[2], x, DEC);
    envoyer_arduino_micro(x); envoyer_arduino_micro(" | ");
    itoa(commandesServos[3], x, DEC);
    envoyer_arduino_micro(x); envoyer_arduino_micro(" | ");   
    envoyer_arduino_micro("\n");
  }
}



void cmdHandle(int incomingByte){
  
  //prevent from lost-sync
  if(incomingByte == SYNC_WORD){
    cmdState = ST_1_CMD;
    return;
  }
  if(incomingByte == IGNORE_00){
    return;
  }
  
  switch(cmdState){
    case ST_1_CMD:{
          switch(incomingByte){
            case CMD_01_LEDON:
                digitalWrite(pinLED, HIGH);
                break;
            case CMD_02_LEDOFF:
                digitalWrite(pinLED, LOW);
                break;
            case CMD_03_TEXT:
                for(int i=0; i < MAX_LENGTH; i++){
                  data[i] = 0;
                }
            
                cmdState = ST_2_LENGTH;
                dataIndex = 0;
                break;
            default:
                cmdState = ST_0;
          }
        }
        break;
    case ST_2_LENGTH:{
        dataLength = incomingByte;
        if(dataLength > MAX_LENGTH){
          dataLength = MAX_LENGTH;
        }
        cmdState = ST_3_DATA;
        }
        break;
    case ST_3_DATA:{
          data[dataIndex] = incomingByte;
          dataIndex++;
          
          //Décomenter la ligne suivante pour que l'Arduino répète tout ce qu'il reçoit.
          //Serial.write(incomingByte);
          
          if(dataIndex==dataLength){
            cmdState = ST_0;
          }
        }
        break;
  }
  
}