#include <Arduino.h>
#include <SPI.h>
#include <SPIMemory.h>
#include "wiring_private.h"

/* Defs */
#if defined(ARDUINO_SAMD_ZERO) && defined(SERIAL_PORT_USBVIRTUAL)
// Required for Serial on Zero based boards
#define Serial SERIAL_PORT_USBVIRTUAL
#endif
#define FOOT_PIN     10  // Foot switch pin
#define LED_R_PIN    11  // Led welding indicator pin
#define LED_G_PIN    2   // Led armed indicator pin
#define WELD_PIN     12   // Switching relay pin

/* State var */
int debug_mode = 1; // | (0) debug off |(1) debug bt | (2) debug states | (3) debug all |
bool armed = false;
bool is_foot_down = false;
bool temp_is_foot_down = false;

/* Parameters, if storing larger data sets, use arrays for byte boundries */
int weld_phase_amount = 1; // Recommended 2,4,6,8 phases
int pause_time = 150;    // Recommended 50-150ms
int weld_time = 80;     // Recommended 20-100ms

/* Bluetooth UART connection */
Uart Serial2(&sercom4, A1, A2, SERCOM_RX_PAD_1, UART_TX_PAD_0); // A1(TX) on SERCOM4.0, A2(RX) on SERCOM4.1
/* Flash serial controller through SPI1 (SERCOM1) */
SPIFlash flash(SS1, &SPI1); 

void setup() {
  // Begin Serial connections
  Serial.begin(9600);
  while(!Serial);
  Serial2.begin(9600);
  while(!Serial2);
  
  flash.begin();

  delay(50);
  Serial.print(F("Initialising"));
  for (uint8_t i = 0; i < 10; ++i)
  {
    Serial.print(F("."));
  }
  Serial.println();

  if (flash.error()) {
    Serial.println(flash.error(VERBOSE));
  }
  
  // Assign pins A1 & A2 to SERCOM functionality
  pinPeripheral(A1, PIO_SERCOM_ALT);
  pinPeripheral(A2, PIO_SERCOM_ALT);

  // Set up pins
  pinMode(FOOT_PIN, INPUT);
  pinMode(LED_R_PIN, OUTPUT);
  pinMode(LED_G_PIN, OUTPUT);
  pinMode(WELD_PIN, OUTPUT);

  // Init output pins
  digitalWrite(LED_R_PIN, LOW);
  digitalWrite(LED_G_PIN, LOW);
  digitalWrite(WELD_PIN, LOW);

  
  
  // Wait for BT serial
  while (!Serial2)
  {
    printlnDebug("Bluetooth serial not ready...", 1);
    delay(250);
  }

  Serial.println("Arduino ready for BT connection");

  // flash.eraseChip(); // ***!!!Use on first upload to new baord for clean flash chip!!!***
  loadSavedSettings(); // Get stored settings on flash chip

  Serial.println("Done loading settings");

}


/* Runtime loop works off basic arm/shoot/rearm
 *  - Starts in armed state, waits for pedal press and shoots the spot welder
 *  - Pauses and waits for rearm
 *  - Repeat
 */
void loop() {
  if (Serial2.available())
  {
    handleBTData();
  }


  is_foot_down = digitalRead(FOOT_PIN);
  printDebug("Foot State: " + String(is_foot_down) + " | ", 2);
  
  if (armed)
  { // If armed and pedal pressed then shoot
    printlnDebug("Welder State: ARMED!", 2);
    digitalWrite(LED_G_PIN, HIGH); // Armed led indicator
    
    if (is_foot_down)
    {
      armed = false;
      digitalWrite(LED_G_PIN, LOW); // Green led off
      weld();
    }
  }

  if (!armed) 
  { // If not armed, and pedal not pressed then rearm
    printlnDebug("Welder State: DISARMED!", 2);
    if (!is_foot_down) 
    {
      armed = true;
    }
  }

  delay(1); // Delay for stability
}

void handleBTData(){  // Get command code at beginning of string
  printlnDebug("\n\nBT handle START...", 1);
  String btString = Serial2.readString();
  int code = getCommandCode(btString);
  
  printlnDebug("\tCode received: " + String(code), 1);

  // Decode bt serial data
  if (code == 100)
  { // Upload welding parameters to app
    printlnDebug("\tUploading paramaters... ", 1);
    Serial2.print("[" + String(weld_phase_amount) + "," + String(pause_time) + "," + String(weld_time) + "]");
    printlnDebug("\t\tSent [" + String(weld_phase_amount) + "," + String(pause_time) + "," + String(weld_time) + "]", 1);
    delay(1000);
  }
  else if (code == 200)
  { // Download welding parameters from app
    printlnDebug("\tDownloading paramaters... ", 1);
    printlnDebug("\t" + btString, 1);
    int params[3];
    parseStringArray(params, btString);
    
    printlnDebug("\t\tweld_phase_amount: " + String(params[0]), 1); 
    printlnDebug("\t\tpause_time: " + String(params[1]), 1); 
    printlnDebug("\t\tweld_time: " + String(params[2]), 1);

    weld_phase_amount = params[0];
    pause_time = params[1];
    weld_time = params[2];

    saveLocalSettings();
  }
  else
  {
    printlnDebug("\t\tERROR: Unknown code: " + String(code), 1);
  }

  printlnDebug("BT handle END...", 1);
  printlnDebug("", 1);
}

int getCommandCode(String encodedBTString){
  printDebug("\tGetting command code...", 1);
  String tempCodeString = "";
  int charIndex = 0;
  while (charIndex < encodedBTString.length()) {
    if (isDigit(encodedBTString[charIndex])) {   // Get first int in string char by char
      tempCodeString += encodedBTString[charIndex];
    }
    if (encodedBTString[charIndex+1] == ':'){  // Once end of command code exit and return it
      break;
    }

    charIndex++;
  }

  return tempCodeString.toInt();
}

void parseStringArray(int arr[], String encoded_array){  // Parse array string for 3 settings parameters
  String tempNumString = "";
  int charIndex = 0;
  int paramIndex = 0;
  
  // Parse past command code
  while (charIndex < encoded_array.length()){
    if (encoded_array[charIndex] == ':'){
      charIndex++;
      break;
    }
    charIndex++;
  }
  
  while (charIndex < encoded_array.length())
  {
    if (isDigit(encoded_array[charIndex])) //Find first number
    {
      tempNumString += encoded_array[charIndex]; //Add number char to temp string
      
      if (!isDigit(encoded_array[charIndex+1])) //If current char is last digit in sequence, save string int
      {
        arr[paramIndex] = tempNumString.toInt(); //Save string int as int to array
        printlnDebug("\t\tFound int: " + tempNumString, 1);
        tempNumString = "";
        paramIndex++; //Save next array index next time
      }
      
    }

    charIndex++; //Move to next character
  }
 
}

void weld() {  // Do actual spot welding action
  printlnDebug("\n\nWelding START...", 2);
  printlnDebug("\tRed LED On!", 2);
  digitalWrite(LED_R_PIN, HIGH); // Red led on

  for (int i=0; i<weld_phase_amount; i++){
    // Weld start...
    printDebug("\t\tWelding...", 2);
    digitalWrite(WELD_PIN, HIGH);
    delay(weld_time);
    printlnDebug("\t\tWeld Done", 2);
    // Weld end...

    // Pause start...
    printDebug("\t\tPause...", 2);
    digitalWrite(WELD_PIN, LOW);
    delay(pause_time);
    printlnDebug("\t\tPause Done", 2);
    // Pause end...
  }

  digitalWrite(WELD_PIN, LOW);
  digitalWrite(LED_R_PIN, LOW); // Red led off
  printlnDebug("\tRed LED Off!", 2);
  printlnDebug("Welding END...", 2);
  printlnDebug("", 2);

}


void loadSavedSettings(){   // Load saved settings from the on-board flash storage
  Serial.println();
  Serial.println();
  Serial.println("Reading saved settings...");
  
  weld_phase_amount = flash.readWord(0);
  pause_time = flash.readWord(2);
  weld_time = flash.readWord(4);
  Serial.print("\tWeld Phase Amount: \t "); Serial.println(weld_phase_amount);
  Serial.print("\tPause Time: \t "); Serial.println(pause_time);
  Serial.print("\tWeld Time: \t "); Serial.println(weld_time);
  
  Serial.println("Settings stored locally...reading finished!"); 
  Serial.println();
  Serial.println();
}

void saveLocalSettings(){   // Write current local settings to on-board flash storage
  Serial.println();
  Serial.println();
  Serial.println("Writing local settings...");
  Serial.print("\ttWeld Phase Amount: \t "); Serial.println(weld_phase_amount);
  Serial.print("\tPause Time: \t "); Serial.println(pause_time);
  Serial.print("\tWeld Time: \t "); Serial.println(weld_time);
  flash.eraseSection(0, 6);
  if (flash.writeWord(0, weld_phase_amount)) {
    Serial.println("weld_phase_amount write SUCCESS!");
  }
  else {Serial.println("weld_phase_amount write FAIL...");}

  if (flash.writeWord(2, pause_time)) {
    Serial.println("pause_time write SUCCESS!");
  }
  else {Serial.println("pause_time time write FAIL...");}
  
  if (flash.writeWord(4, weld_time)){
    Serial.println("weld_time write SUCCESS!");
  }
  else {Serial.println("weld_time write FAIL...");}
}

void printlnDebug(String my_string, int debug_type){   // Println debug statements
  if (debug_mode == debug_type || debug_mode == 3) 
  {
    Serial.println(my_string);
  }
}
void printDebug(String my_string, int debug_type){   // Print debug statements
  if (debug_mode == debug_type || debug_mode == 3) 
  {
    Serial.print(my_string);
  }
}

/* Handler for SERCOM4 (Serial2) */
void SERCOM4_Handler(){
  Serial2.IrqHandler();
}
