/**
* Program to configure the Bluetooth module in AT_Mode: 
*  - Switch the AT mode to 1 before turning on Arduino
*  - Use commmands from the AT mode
*    ref(http://www.dfrobot.com/image/data/TEL0026/TEL0026_Datasheet.pdf )
*/

void setup()
{

  Serial.begin(38400);
  Serial.println("Enter AT commands:");
  
  //btSerial.begin(38400);
  
}

void loop()
{

  // Info from Comm1 is written in the BT comm 
  /**if (Serial.available()){
    btSerial.write(Serial.read());
  }
  
  // Info from BT is displayed in Comm1
  if (btSerial.available()){
    Serial.write(btSerial.read());
  }**/
}

