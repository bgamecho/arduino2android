/*
* Simple program to chat with Arduino MEGA
*/

void setup()
{
  Serial.begin(57600); //COM serial 
  Serial1.begin(57600); //Bluetooth module serial
  
  Serial.println("Linea Serie");
}

void loop()
{
  if(Serial1.available()){
    Serial.write(Serial1.read());
  }
  if(Serial.available()){
    Serial1.write(Serial.read());
  }

}
