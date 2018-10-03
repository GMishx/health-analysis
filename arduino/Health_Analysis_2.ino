/**
 * Copyright 2018, Gaurav Mishra <gmishx@gmail.com>
 */

#include <SoftwareSerial.h>
#include <EEPROM.h>
#include <ArduinoJson.h>
#include "Adafruit_FONA.h"

#include <MsTimer2.h>

#define BLUE_TX 4                   // Tx pin of HC-05
#define BLUE_RX 5                   // Rx pin of HC-05
#define GSM_RX 2                    // Rx pin of GSM
#define GSM_TX 3                    // Tx pin of GSM
#define RST 7                       // RST pin of GSM
#define STATE_PIN 11                // Pin to check BT state
#define LOCK_PIN 8                  // Pin to control lock
#define pulsePin A0                 // Pulse Sensor purple wire connected to analog pin 0
#define speedPin A1                 // Reed Switch pin to analog 1
#define pin_X A2                    // X of accelerometer
#define pin_Y A3                    // Y of accelerometer
#define pin_Z A4                    // Z of accelerometer

SoftwareSerial BlueSS = SoftwareSerial(BLUE_TX, BLUE_RX);
SoftwareSerial fonaSS = SoftwareSerial(GSM_TX, GSM_RX);
Adafruit_FONA fona = Adafruit_FONA(RST);

class PulseThread
{
    int BPM, Signal, IBI, P, T, thresh, amp, pulsePin;
    boolean Pulse, QS;
    int rate[10];
    unsigned long sampleCounter, lastBeatTime;
    boolean firstBeat = true;
    boolean secondBeat = false;

  public:
    PulseThread(int pin) {
      pulsePin = pin;
      BPM = 0;
      IBI = 600;
      Pulse = QS = false;
      sampleCounter = lastBeatTime = 0;
      P = T = 512;
      thresh = 525;
      amp = 100;
      firstBeat = true;
      secondBeat = false;
    }

    int getBPM() {
      return BPM;
    }

    void run() {
      // Reads the analog pin, and saves it localy
      Signal = analogRead(pulsePin);              // read the Pulse Sensor
      sampleCounter += 2;                         // keep track of the time in mS with this variable
      int N = sampleCounter - lastBeatTime;       // monitor the time since the last beat to avoid noise

      //  find the peak and trough of the pulse wave
      if (Signal < thresh && N > (IBI / 5) * 3) { // avoid dichrotic noise by waiting 3/5 of last IBI
        if (Signal < T) {                       // T is the trough
          T = Signal;                         // keep track of lowest point in pulse wave
        }
      }

      if (Signal > thresh && Signal > P) {        // thresh condition helps avoid noise
        P = Signal;                             // P is the peak
      }                                        // keep track of highest point in pulse wave

      //  NOW IT'S TIME TO LOOK FOR THE HEART BEAT
      // signal surges up in value every time there is a pulse
      if (N > 250) {                                  // avoid high frequency noise
        if ( (Signal > thresh) && (Pulse == false) && (N > (IBI / 5) * 3) ) {
          Pulse = true;                               // set the Pulse flag when we think there is a pulse
          IBI = sampleCounter - lastBeatTime;         // measure time between beats in mS
          lastBeatTime = sampleCounter;               // keep track of time for next pulse

          if (secondBeat) {                      // if this is the second beat, if secondBeat == TRUE
            secondBeat = false;                  // clear secondBeat flag
            for (int i = 0; i <= 9; i++) {       // seed the running total to get a realisitic BPM at startup
              rate[i] = IBI;
            }
          }

          if (firstBeat) {                       // if it's the first time we found a beat, if firstBeat == TRUE
            firstBeat = false;                   // clear firstBeat flag
            secondBeat = true;                   // set the second beat flag
            return;                              // IBI value is unreliable so discard it
          }


          // keep a running total of the last 10 IBI values
          word runningTotal = 0;                  // clear the runningTotal variable

          for (int i = 0; i <= 8; i++) {          // shift data in the rate array
            rate[i] = rate[i + 1];                // and drop the oldest IBI value
            runningTotal += rate[i];              // add up the 9 oldest IBI values
          }

          rate[9] = IBI;                          // add the latest IBI to the rate array
          runningTotal += rate[9];                // add the latest IBI to runningTotal
          runningTotal /= 10;                     // average the last 10 IBI values
          BPM = 60000 / runningTotal;             // how many beats can fit into a minute? that's BPM!
          QS = true;                              // set Quantified Self flag
          // QS FLAG IS NOT CLEARED INSIDE THIS ISR
        }
      }

      if (Signal < thresh && Pulse == true) {  // when the values are going down, the beat is over
        Pulse = false;                         // reset the Pulse flag so we can do it again
        amp = P - T;                           // get amplitude of the pulse wave
        thresh = amp / 2 + T;                  // set thresh at 50% of the amplitude
        P = thresh;                            // reset these for next time
        T = thresh;
      }

      if (N > 2500) {                          // if 2.5 seconds go by without a beat
        thresh = 512;                          // set thresh default
        P = 512;                               // set P default
        T = 512;                               // set T default
        lastBeatTime = sampleCounter;          // bring the lastBeatTime up to date
        firstBeat = true;                      // set these to avoid noise
        secondBeat = false;                    // when we get the heartbeat back
        BPM = 0;
      }
    }
};

class SpeedThread
{
    int speedPin, maxReedCounter, reedCounter;
    float peed, radius, circumference;

    long timer;

  public:
    SpeedThread(int pin, float rad) {
      speedPin = pin;
      peed = timer = 0;
      radius = rad;
      circumference = 2 * 3.14 * radius * 0.0254; //Circumference in meteres
      reedCounter = maxReedCounter = 100;
    }

    float getSpeed() {
      return peed;
    }

    void run() {
      int reedVal = analogRead(speedPin);         //get val of A0
      reedVal = (reedVal < 80) ? 1 : 0;

      if (reedVal) { //if reed switch is closed
        if (reedCounter == 0) { //min time between pulses has passed
          peed = 3600 * float(circumference) / float(timer);
          //peed = 1.609344 * ((56.8 * float(circumference)) / float(timer)); //calculate km per hour
          timer = 0;//reset timer
          reedCounter = maxReedCounter;//reset reedCounter
        }
        else {
          if (reedCounter > 0) { //don't let reedCounter go negative
            reedCounter -= 1;//decrement reedCounter
          }
        }
      }
      else { //if reed switch is open
        if (reedCounter > 0) { //don't let reedCounter go negative
          reedCounter -= 1;//decrement reedCounter
        }
      }
      if (timer > 2000) {
        peed = 0;//if no new pulses from reed switch- tire is still, set speed to 0
      }
      else {
        timer += 1;//increment timer
      }
    }
};

class uploadThread
{
    PulseThread *pt;
    SpeedThread *st;
    SoftwareSerial *BlueeSS;
  public:
    uploadThread(SoftwareSerial *bb, PulseThread *p, SpeedThread *s) {
      pt = p;
      st = s;
      BlueeSS = bb;
    }
    void run() {
      noInterrupts();
      StaticJsonBuffer<100> jsonBuffer;
      JsonObject& root = jsonBuffer.createObject();
      root["BPM"] = pt->getBPM();
      root.set<double>("speed", st->getSpeed());
      //root.prettyPrintTo(Serial);
      char json[100] = "";
      root.printTo(json, sizeof(json));
      BlueeSS->println(json);
      BlueeSS->flush();
      interrupts();
    }
};

//  Variables
volatile int loc_pos[3];                            //Store the position of bike when locked
//static const float thresh = 0.08;                   //Tollerance to shakeing of accelerometer
static int thresh[3];                               //Hold treshold values for all axis
long g = 10, x, a, b, k;                            //g->Password,x->rand no,a->secret calc,b->secret received,k->key generated
static const long prime = 9973;                     //Prime number for calculations
int i = 0;                                          //Index of Data array
static const int address = 100;                     //Address to store g
static const int PHaddress = 600;                   //Address to store phone number
char num[11], Data[15], character;                  //num->phone no,Data->digit from Blue,character->char from Blue
volatile unsigned long sentTime = 0UL;              //Last SMS sent time
volatile boolean lockit, locked, conn, ud;          //lockit->bike to be locked,locked->bike locked,conn->phone connected,ud->data requested
boolean first = true;                               //First SMS
volatile boolean stopp = false, upuser = false;
char buff[255];                                     //Temporary buffer to store inputs from phone
byte bi = 0;                                        //Index for buffer
volatile boolean smsFirstTry = true;                //First time SMS is tried
volatile unsigned long firstTryMill;                //Time of first try
static const char message[] = "You bicycle is being tampered! Please go and check it now!";

boolean stable(boolean mode = false) {              //Function to check if bike is stable/moving
  bool st = true;                                   //Return variable
  int last_pos[3];
  get_pos(last_pos);                                //Get current vector of accel
  if (mode) {                                       //If asked for theft attempt
    st = compareVectors(loc_pos, last_pos);         //Compare with lock position
  }
  else {                                            //Asked during locking
    int curr_pos[3];
    for (byte i = 0; i < 3; i++) {                  //Take 3 readings
      delay(100);                                   //Wait for 100ms
      get_pos(curr_pos);                            //Update current pos
      st &= compareVectors(last_pos, curr_pos);     //Compare current and last pos; if any false, keep false
      for (byte j = 0; j < 3; j++) {                //Update last pos with current
        last_pos[j] = curr_pos[j];
      }
    }
  }
  return st;
}

boolean compareVectors(int a[], int b[]) {          //Compare vector a with b
  boolean ret = true;
  for (byte i = 0; i < 3; i++) {
    if (((a[i] + thresh[i]) < b[i]) || ((a[i] - thresh[i]) > b[i])) { //b is outof range?
      ret = false;
    }
  }
  return ret;
}

void calibrate() {
  int threshh[6];
  threshh[0] = threshh[2] = threshh[4] = 1024;      //Minimum values
  threshh[1] = threshh[3] = threshh[5] = 0;         //Maximum values
  for (byte k = 0; k < 100; k++) {                  //Take 100 readings
    int temp[3];
    get_pos(temp);                                  //Get new reading
    word i = 0, j = 0;
    while (j < 3) {                                 //Compare the min and max for 3 axis
      if (threshh[i] > temp[j]) {                   //If min > curr
        threshh[i] = temp[j];                       //Update Min
      }
      i++;                                          //Goto next value
      if (threshh[i] < temp[j]) {                   //If max < curr
        threshh[i] = temp[j];                       //Update Max
      }
      i++; j++;                                     //Goto next axis
    }
    delay(100);
  }
  thresh[0] = (abs(threshh[0] - threshh[1]) / 2) + 25;
  thresh[1] = (abs(threshh[2] - threshh[3]) / 2) + 25;
  thresh[2] = (abs(threshh[4] - threshh[5]) / 2) + 25;
  /*Serial.println("Callibrated values:");
    Serial.print(thresh[0]);
    Serial.print(",");
    Serial.print(thresh[1]);
    Serial.print(",");
    Serial.println(thresh[2]);*/
  Serial.println("Ready");
}

PulseThread pt = PulseThread(pulsePin);             //Initialise PulseThread
SpeedThread st = SpeedThread(speedPin, 13.5);       //Initialise SpeedThread
uploadThread ut = uploadThread(&BlueSS, &pt, &st);  //Initialise uploadThread

// This is the callback for the Timer
unsigned int tt = 0;                                //Time passed since last call
void timerCallback() {                              //function called every 1ms
  tt += 1;                                          //1 ms passed since last call
  if (ud) {                                         //If data required
    if (tt % 2 == 0) {                              //Call every 2ms
      pt.run();                                     //Update pulse
    }
    st.run();                                       //Update speed, called every 1ms
  }
  if (tt % 600) {                                   //Every 200ms
    process_Blue();                                 //Read data from phone
  }
  if (tt >= 1000) {                                 //1000ms passed (1sec)
    tt = 0;                                         //Reset to 0
    if (ud) {                                       //If data required
      ut.run();                                     //Send data to phone
    }
    if (locked) {                                   //If bike locked
      if (!stable(true)) {                          //If bike is not stable->theft
        EEPROMReadStr(PHaddress, num);              //Read phone no from memory
        upuser = true;                              //Send SMS to phone
      }
    }
    if (lockit & !locked) {                         //If device to be locked and not locked yet
      _lock();                                      //Try to lockit
    }
  }
  if (!conn) {                                      //If mobile disconnected
    lock();                                         //Safe lock the bike
  }
}

void setup() {                                      //Initialing device
  pinMode(RST, OUTPUT);                             //Reset pin for GSM is an OUTPUT
  digitalWrite(RST, HIGH);                          //Make it HIGH to keep it from turning on/off
  randomSeed(analogRead(5));                        //Get random number properly
  pinMode(pulsePin, INPUT);                         //pulsePin is INPUT
  pinMode(speedPin, INPUT_PULLUP);                  //seedPin in INPUT default to HIGH
  pinMode(LOCK_PIN, OUTPUT);                        //LOCK_PIN is OUTPUT
  pinMode(STATE_PIN, INPUT);                        //STATE_PIN is INPUT
  pinMode(pin_X, INPUT);                            //pin_X,Y,Z are INPUT
  pinMode(pin_Y, INPUT);
  pinMode(pin_Z, INPUT);

  Serial.begin(115200);                             //Serial communication for debuging
  BlueSS.begin(115200);                             //Bluetooth Serial
  //fonaSS.begin(4800);
  fonaSS.begin(115200);

  delay(1000);                                      //Wait for 1sec to let device stablise
  //Serial.println("CALL Called");
  calibrate();                                      //Calibrate accel
  get_pos(loc_pos);                                 //Update lock position

  lockit = locked = conn = ud = false;              //Initialise all to false

  lock();                                           //attempt to lock the device

  //initTimer();                                      //Start calling
  MsTimer2::set(1, timerCallback);                  // 1ms period
  MsTimer2::start();
}

/*void initTimer() {
  Timer1.initialize(1000);                          //Call every 1000ns (1ms)
  Timer1.attachInterrupt(timerCallback);            //Function to be called
  Timer1.start();                                   //Start calling
  }*/

void loop() {
  MsTimer2::stop();
  BlueSS.listen();                                  //Make Bluetooth serial listen
  while (BlueSS.available()) {                      //Till there is data with Bluetooth
    buff[bi++] = BlueSS.read();                     //Read the data and store in buffer
  }
  BlueSS.flush();                                   //Flush the Bluetooth input
  MsTimer2::start();
  if (upuser) {
    MsTimer2::stop();
    if ((sentTime + 120000UL <= millis()) || first) { //If 2 minutes passed since last SMS sent || it is first SMS
      first = false;                                  //SMS already sent
      fonaSS.listen();
      for (byte x = 0; x < 5 & ! fona.begin(fonaSS); x++) {
        digitalWrite(RST, LOW);
        delay(5000);
        digitalWrite(RST, HIGH);
        delay(5000);
      }
      if (x == 5) {
        goto sent;
      }
      delay(5000);
      char sendto[15];
      sendto[0] = '+';
      sendto[1] = '9';
      sendto[2] = '1';
      int j = 3;
      while (j < 15) {
        sendto[j++] = num[j - 4];
      }
      sendto[j++] = '\0';
      //for (byte s = 0; s < 3 & !fona.sendSMS(sendto, message); s++);
      fona.sendSMS(sendto, message);
      fona.println("AT+CPOWD=1");
      goto sent;
    }
    //Timer1.attachInterrupt(timerCallback);
sent: sentTime = millis();
    upuser = false;
    BlueSS.listen();
    MsTimer2::start();
    return;
  }
}

//  Where the Magic Happens
void process_Blue() {                               //Process inputs from phone
  conn = connec();                                  //Check if phone connected
  if (!conn) {                                      //If not connected
    return;                                         //Can't process, return
  }
  /*for (byte j = 0; j < bi; j++) {                   //Print to user for debugging
    Serial.println(buff[j]);
    }*/
  for (byte bj = 0; bj < bi; bj++) {                //Process each character in buffer
    character = buff[bj];                           //Take each character from buffer
    if (isDigit(character)) {                       //If it is a digit
      Data[i++] = character;                        //Add to Data buffer
    }
    else if (character == 'g') {                    //If user updated password
      g = getLong(i);                               //Read the password in g
      i = 0;
      //Serial.print("G= ");
      //Serial.println(g);
      EEPROMWritelong(address, g);                  //Update password in memory
    }
    else if (character == 'p') {                    //If user updated phone no
      Data[i] = '\0';                               //Add delimiter to the number
      //Serial.print("P= ");
      //Serial.println(Data);
      EEPROMWriteStr(PHaddress, Data);              //Update the number in memory
      i = 0;
    }
    else if (character == 'e') {                    //If user attempts to unlock bike
      g = EEPROMReadlong(address);                  //Get password from memory
      b = getLong(i);                               //Get secret b sent by user
      i = 0;
      x = random(prime);                            //Generate x
      a = modpow(g, x, prime);                      //Generate secret a
      k = modpow(b, x, prime);                      //Generate key
      BlueSS.print(a);                              //Send secret a to phone
      BlueSS.println("}");                          //Inform end with a delimiter
      /*Serial.println("g,x,a,b,prime,k");            //Debugging
        Serial.print(g);
        Serial.print(",");
        Serial.print(x);
        Serial.print(",");
        Serial.print(a);
        Serial.print(",");
        Serial.print(b);
        Serial.print(",");
        Serial.print(prime);
        Serial.print(",");
        Serial.println(k);*/
    }
    else if (character == 'k') {                    //User send key for authentication
      long k2 = getLong(i);                         //Get the key from Bluetooth
      i = 0;
      //Serial.print("k2=");
      //Serial.println(k2);
      //Serial.println(k == k2);
      BlueSS.print("{");                            //Inform phone about resonse
      if (k == k2) {                                //User authenticated
        BlueSS.print("1");                          //Reply with true
        unlock();                                   //Unlock the bike
      }
      else {
        BlueSS.print("0");                          //Reply with false
        lock();                                     //Lock the bike
      }
      BlueSS.println("}");                          //Delimit the response
    }
    else if (character == 'l') {                    //If user wants to lock the bike
      i = 0;
      lock();                                       //Attempt to lock it
    }
    else if (character == 'r') {                    //If user requests data
      i = 0;
      ud = true;                                    //Set the request
    }
    else if (character == 't') {                    //If user stops the data request
      i = 0;
      ud = false;                                   //Unset the request
    }
    /*for (int j = 0; j < i; j++) {                   //Debugging to print digit data
      Serial.println(Data[j]);
      }*/
  }
  bi = 0;
}

void lock() {                                       //Attempt to lock the bike
  lockit = true;                                    //Set bike to be locked
}

void _lock() {                                      //Lock the bike
  if (!locked) {                                    //If bike is not yet locked
    if (stable()) {                                 //If bike is stable (for safety)
      get_pos(loc_pos);                             //Update the lock position
      digitalWrite(LOCK_PIN, HIGH);                 //Lock the bike
      locked = true;                                //Update lock status
      lockit = false;                               //Unset the lock request
    }
  }
}

void unlock() {                                     //Unlock the bike
  if (locked) {                                     //If bike is locked
    digitalWrite(LOCK_PIN, LOW);                    //Unlock the bike
    locked = false;                                 //Update the lock status
  }
}

boolean connec() {                                  //Phone still connected
  word st = digitalRead(STATE_PIN);                 //Get the status
  if (st) {                                         //If connected
    conn = true;
  }
  else {                                            //If disconnected
    conn = false;
    ud = false;
  }
  return conn;
}

void get_pos(int f[]) {                             //Updating accel position
  unsigned long sum = 0;
  for (byte i = 0; i < 5; i++) {
    sum += analogRead(pin_X);                       //Get x-axis reading
    delay(10);
  }
  f[0] = sum / 5; sum = 0;
  for (byte i = 0; i < 5; i++) {
    sum += analogRead(pin_Y);                       //Get y-axis reading
    delay(10);
  }
  f[1] = sum / 5; sum = 0;
  for (byte i = 0; i < 5; i++) {
    sum += analogRead(pin_Z);                       //Get z-axis reading
    delay(10);
  }
  f[2] = sum / 5;
}

long int modpow(long int base, long int expo, long int modulus) {   //Calculate (base^x%modulus)
  base %= modulus;
  long int result = 1;
  while (expo > 0) {
    if (expo & 1) result = (result * base) % modulus;
    base = (base * base) % modulus;
    expo >>= 1;
  }
  return result;
}

void EEPROMWritelong(int address, long value)
{
  //Decomposition from a long to 4 bytes by using bitshift.
  //One = Most significant -> Four = Least significant byte
  byte four = (value & 0xFF);
  byte three = ((value >> 8) & 0xFF);
  byte two = ((value >> 16) & 0xFF);
  byte one = ((value >> 24) & 0xFF);

  //Write the 4 bytes into the eeprom memory.
  EEPROM.update(address, four);
  EEPROM.update(address + 1, three);
  EEPROM.update(address + 2, two);
  EEPROM.update(address + 3, one);
}

long EEPROMReadlong(long address)
{
  //Read the 4 bytes from the eeprom memory.
  long four = EEPROM.read(address);
  long three = EEPROM.read(address + 1);
  long two = EEPROM.read(address + 2);
  long one = EEPROM.read(address + 3);

  //Return the recomposed long by using bitshift.
  return ((four << 0) & 0xFF) + ((three << 8) & 0xFFFF) + ((two << 16) & 0xFFFFFF) + ((one << 24) & 0xFFFFFFFF);
}

void EEPROMReadStr(long address, char str[]) {
  for (int j = 0; j < 11; j++)
    str[j] = EEPROM.read(address + j);
}

void EEPROMWriteStr(long address, char str[]) {
  for (int j = 0; j < 11; j++)
    EEPROM.update(address + j, str[j]);
}

unsigned long getLong(int i) {
  unsigned long ret = 0;
  for (int j = 0; j < i; j++) {
    ret *= 10;
    ret += ((int)Data[j] - 48);
  }
  return ret;
}
