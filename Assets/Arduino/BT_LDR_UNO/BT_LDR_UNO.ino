#include <avr/interrupt.h>  
#include <avr/io.h>
#include <SoftwareSerial.h>

#define rxPin 2
#define txPin 3

// Configura un nuevo puerto serie
SoftwareSerial miSerial = SoftwareSerial(rxPin, txPin);

//A0 (Analog 0) pin is an Light Diode Resistor (LDR)
int ldr =0;

boolean flag_seconds = false;
int seconds = 0;
int ticks = 0;

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
  miSerial.begin(57600);

  Serial.println("Linea Serie");

}

void loop()
{
  
  if(miSerial.available()){
    Serial.write(miSerial.read());
  }
  if(Serial.available()){
    miSerial.write(Serial.read());
  }  
  if(check_clock()){ 
    ldr = analogRead(0);
  
    miSerial.print("T:");
    miSerial.print(seconds);
    miSerial.print("-");
  
    miSerial.print("LDR:");
    miSerial.print(ldr);
    miSerial.println("");
    
    Serial.print("T:");
    Serial.print(seconds);
    Serial.print("-");
    Serial.print("LDR:");
    Serial.print(ldr);
    Serial.println("");
  }
}

// This method updates a counter of seconds and shows information throught the Serial interface
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
  if(ticks > 999){
    ticks = 0;           //Resets the interrupt counter
    flag_seconds = true;
    seconds++;
  }
  TCNT2 = 130;           //Reset Timer to 130 out of 255
  TIFR2 = 0x00;          //Timer2 INT Flag Reg: Clear Timer Overflow Flag
};  
