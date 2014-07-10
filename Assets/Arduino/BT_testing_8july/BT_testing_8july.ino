/*
* Simple program to send information throught the serial bluetooth.
* Every second the Led in the arduino board switches and a message is printed
* in both, the comm (pin 0,1) and bt (pins 2,3) serial interfaces. 
* Be carefull, this example uses the Timer 2 of Arduino UNO Rev3, check the 
* documentation if you use other model of Arduino. 
*  - Arduino timer 2, refs: 
*     * http://arduinomega.blogspot.pt/2011/05/timer2-and-overflow-interrupt-lets-get.html
*     * http://real2electronics.blogspot.pt/2011/01/timer-2.html 
*/

#include <avr/interrupt.h>  
#include <avr/io.h>

#include <SoftwareSerial.h>

#define rxPin 2
#define txPin 3

// Pin 13 is a LED 
int led = 10;

// Bluetooth seria communication pin 2 and 3
SoftwareSerial btSerial =  SoftwareSerial(rxPin, txPin);

boolean flag_seconds = false;
int seconds = 0;
int ticks = 0;

int ldr =0;

int led_state = 0;

boolean start = false;

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


// the setup routine runs once when you press reset:
void setup() {
  
  // initialize the digital pin as an output.
  pinMode(led, OUTPUT);
  
  Serial.begin(57600);
  btSerial.begin(57600);
  
  Serial.println("COMM: KAIXO!...");
  btSerial.println("BT: KAIXO!...");
  
  //Setup Timer2 to fire every 1ms
  TCCR2B = 0x00;        //Disbale Timer2 while we set it up
  TCNT2  = 130;         //Reset Timer Count to 130 out of 255
  TIFR2  = 0x00;        //Timer2 INT Flag Reg: Clear Timer Overflow Flag
  TIMSK2 = 0x01;        //Timer2 INT Reg: Timer2 Overflow Interrupt Enable
  TCCR2A = 0x00;        //Timer2 Control Reg A: Wave Gen Mode normal
  TCCR2B = 0x05;        //Timer2 Control Reg B: Timer Prescaler set to 128
  
}

// the loop routine runs over and over again forever:
void loop() {  
  
  if(check_clock()){ 
 
    ldr = analogRead(0);
    
    /**
    Serial.print("S: ");
    Serial.print(seconds);
    Serial.print(" LDR : ");
    Serial.print(ldr);
    **/
    Serial.print(" Android ");
    if(start){
      Serial.println(" Start ON");
    }else{
      Serial.println(" Start OFF");
    } 
    
    if(start){
      
      btSerial.print("T:");
      btSerial.print(seconds);
      btSerial.print("-");
    
      btSerial.print("LDR:");
      btSerial.print(ldr);
      btSerial.println("");
    }
    
  }
  
    // Info from Comm1 is written in the BT comm 
  if (Serial.available()){
    btSerial.write(Serial.read());
  }
  
  // Info from BT is displayed in Comm1
  if (btSerial.available()){
    char command = btSerial.read();
    switch(command){
      case 's':
        start = true; 
      break;
      case 'l':
        my_code();
      break;
      case 'f':
        start = false;
        //Serial.end();
      break;
    }
    
    Serial.print("Rcv: ");
    Serial.print(command);
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

void my_code(){
     if(led_state == 0){     
      digitalWrite(led, HIGH);   // turn the LED on (HIGH is the voltage level)
      led_state = 1;
    }else{
      digitalWrite(led, LOW);    // turn the LED off by making the voltage LOW 
      led_state = 0;
    } 
    
}


