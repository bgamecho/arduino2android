#include <SoftwareSerial.h>

#define rxPin 2
#define txPin 3

// Configura un nuevo puerto serie
SoftwareSerial miSerial = SoftwareSerial(rxPin, txPin);

void setup()
{
Serial.begin(57600);
miSerial.begin(57600);

Serial.println("Linea Serie");
// Serial2.println("KAIXO");
}

void loop()
{

// if(Serial.available()){
// miSerial.write(Serial.read());
// }

if(miSerial.available()){
  Serial.write(miSerial.read());
}
if(Serial.available()){
  miSerial.write(Serial.read());
}

//miSerial.print("msg:");
//miSerial.print("a");
//miSerial.println("");

//
//delay(100);

}
