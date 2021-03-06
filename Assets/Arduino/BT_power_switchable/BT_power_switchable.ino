#include <avr/interrupt.h>
#include <avr/io.h>
#include <avr/pgmspace.h>
#include <SoftwareSerial.h>

//A0 (Analog 0) pin is an Light Diode Resistor (LDR)
int ldr =0;

#define rxPin 2
#define txPin 3

// Configura un nuevo puerto serie
SoftwareSerial btSerial = SoftwareSerial(rxPin, txPin);

int incomingByte = 0;   // for incoming Bluetooth-serial data

int btPower = 8;
boolean started = false;
boolean flag_seconds = false;
int seconds = 0;
int ticks = 0;

int STX = 0x02;
int MSGID_PING = 0x26;
int MSGID_STRESS= 0x27;
int DLC = 55;
int ETX = 0x03;
String payload = "";
int frameNum = 0;

void setup()
{
    //Setup Timer2 to fire every 1ms
  TCCR2B = 0x00;        //Disbale Timer2 while we set it up
  TCNT2  = 130;         //Reset Timer Count to 130 out of 255
  TIFR2  = 0x00;        //Timer2 INT Flag Reg: Clear Timer Overflow Flag
  TIMSK2 = 0x01;        //Timer2 INT Reg: Timer2 Overflow Interrupt Enable
  TCCR2A = 0x00;        //Timer2 Control Reg A: Wave Gen Mode normal
  TCCR2B = 0x05;        //Timer2 Control Reg B: Timer Prescaler set to 128
  
  Serial.begin(57600);
  btSerial.begin(57600);

  // initialize the digital pin as an output.
  pinMode(btPower, OUTPUT);

}

void loop()
{

  // Echo all incoming Bluetooth data (for PING tests)
  while(btSerial.available())
  {
    incomingByte = btSerial.read();
    btSerial.write(incomingByte);
    
    Serial.print(incomingByte);
    Serial.print(' ');
    if(incomingByte==ETX)
      Serial.println("");

  }
  
  // Info from BT is displayed in Comm1
  if (Serial.available())
  {
    char command = Serial.read();
    switch(command){
      case 'p':
        // Power ON Bluetooth
        digitalWrite(btPower, HIGH);
      break;
      
      case 'f':
        // Power OFF Bluetooth (and stop sending data)
        started = false;
        digitalWrite(btPower, LOW);
      break;
      
      case 's':
        // start sending data
        started = true;
        // Power ON Bluetooth
        digitalWrite(btPower, HIGH);
        break;
        
      case 'm':
        // Mute (stop sending data)
        started = false;
      break;
      
      case 'i':
        // increment Payload size in one byte
        payload+="@";
        break;
        
      case 'r':
        // reset payload to 0 bytes
        payload = "";
        started= false;
        break;
    }
    
    Serial.print("Rcv: ");
    Serial.print(command);
    Serial.println("");
    

  }
  
  if(check_clock())
  { 
    if(started)
    {
      ldr = analogRead(0);
      int length = payload.length(); 
      char payloadBytes[length+1]; // Length (with one extra character for the null terminator)
      payload.toCharArray(payloadBytes, length+1);
      
      sendMessage(MSGID_STRESS, payloadBytes, length);
    }
  }
  delay(50);
}

void sendMessage(int MSGID, char payload[], int length)
{
  unsigned char buf[4];
  buf[0] = (length >> 24) & 0xFF;
  buf[1] = (length >> 16) & 0xFF;
  buf[2] = (length >> 8) & 0xFF;
  buf[3] = (length) & 0xFF;
  
  long crc = crc_string(payload);
  unsigned char crcBytes[sizeof(long int)];
  memcpy(crcBytes,&crc,sizeof(long int));
    
  if(frameNum<99)
    frameNum+=1;
  else
    frameNum = 1;
  
  btSerial.write(STX);
  btSerial.write(MSGID);
  btSerial.write(frameNum);
  btSerial.write(buf, sizeof(buf));
  btSerial.write(payload);
  btSerial.write(crcBytes,sizeof(crcBytes));
  btSerial.write(ETX);
  
  Serial.print(length);
  Serial.print(": ");
  Serial.print(payload);
  Serial.println("");


}

// This method updates a counter of seconds and shows information through the Serial interface
boolean check_clock()
{
  if(flag_seconds){
    flag_seconds = false;
    return true;
  } 
  return false;
}

//Timer2 Overflow Interrupt Vector, called every 1ms
ISR(TIMER2_OVF_vect) {
  ticks++;               //Increments the interrupt counter
  if(ticks > 99){
    ticks = 0;           //Resets the interrupt counter
    flag_seconds = true;
    seconds++;
  }
  TCNT2 = 130;           //Reset Timer to 130 out of 255
  TIFR2 = 0x00;          //Timer2 INT Flag Reg: Clear Timer Overflow Flag
}; 


//---------------------------------------------
//CRC (Checksum for error detection)

static PROGMEM prog_uint32_t crc_table[16] = {
    0x00000000, 0x1db71064, 0x3b6e20c8, 0x26d930ac,
    0x76dc4190, 0x6b6b51f4, 0x4db26158, 0x5005713c,
    0xedb88320, 0xf00f9344, 0xd6d6a3e8, 0xcb61b38c,
    0x9b64c2b0, 0x86d3d2d4, 0xa00ae278, 0xbdbdf21c
};

unsigned long crc_update(unsigned long crc, byte data)
{
    byte tbl_idx;
    tbl_idx = crc ^ (data >> (0 * 4));
    crc = pgm_read_dword_near(crc_table + (tbl_idx & 0x0f)) ^ (crc >> 4);
    tbl_idx = crc ^ (data >> (1 * 4));
    crc = pgm_read_dword_near(crc_table + (tbl_idx & 0x0f)) ^ (crc >> 4);
    return crc;
}

unsigned long crc_string(char *s)
{
  unsigned long crc = ~0L;
  while (*s)
    crc = crc_update(crc, *s++);
  crc = ~crc;
  return crc;
}


