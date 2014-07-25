#include <avr/interrupt.h>
#include <avr/io.h>
#include <SoftwareSerial.h>
#include <avr/pgmspace.h>


//A0 (Analog 0) pin is an Light Diode Resistor (LDR)
int ldr =0;

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
  Serial1.begin(57600);

  // initialize the digital pin as an output.
  pinMode(btPower, OUTPUT);

}

void loop()
{

  // Echo all incoming Bluetooth data (for PING tests)
  if(Serial1.available()){
    incomingByte = Serial1.read();
    
    char aux[1] = {incomingByte};
    sendMessage(MSGID_PING, aux, 2);
  }
  
  // Info from BT is displayed in Comm1
  if (Serial.available()){
    char command = Serial.read();
    switch(command){
      case 's':
        started = true;
        digitalWrite(btPower, HIGH);
      break;
      case 'f':
        started = false;
        digitalWrite(btPower, LOW);
      break;
    }
    
    Serial.print("Rcv: ");
    Serial.print(command);
    Serial.println("");
    

  }
  
  if(check_clock()){ 
    
    if(started){
      ldr = analogRead(0);
      
      String payload = "T:";
      payload+= seconds;
      payload+= "-LDR:";
      payload+=ldr;
      payload+="#";
      payload+="Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut aliquet, ";
      //payload+="Lorem ipsum dolor sit amet, consectetur adipiscing elit.Lorem ipsum dolor sit amet, consectetur adipiscing elit.Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed elit dolor, venenatis ac magna at, dapibus ultricies tellus. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Fusce pharetra turpis non leo pulvinar, at elementum urna hendrerit. ";

      //payload = "HELLO";
      
      int length = payload.length()+1; // Length (with one extra character for the null terminator)
      char aux[length];
      payload.toCharArray(aux, length);
      
      sendMessage(MSGID_STRESS, aux, length);
    }
  }
  delay(50);
}

void sendMessage(int MSGID, char payload[], int length){
  
  long crc = crc_string(payload);
  unsigned char buf[sizeof(long int)];
  memcpy(buf,&crc,sizeof(long int));

  
  Serial1.write(STX);
  Serial1.write(MSGID);
  Serial1.write(length);
  Serial1.write(payload);
  Serial1.write(buf,sizeof(buf));
  Serial1.write(ETX);
  
  Serial.print(length);
  Serial.print(": ");
  Serial.print(payload);
  Serial.println("");
}

// This method updates a counter of seconds and shows information through the Serial interface
boolean check_clock(){
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


