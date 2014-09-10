/* Motor Core

  This code for the Arduino Robot's motor board
  is the stock firmware. program the motor board with 
  this sketch whenever you want to return the motor
  board to its default state.
  
*/

//#include <ArduinoRobotMotorBoard.h>
int 1INA = 8;
int 1INB = 9;
int 1PWM = 10;
int 2INA = 11;
int 2INB = 12;
int 2PWM = 13;

int 1PWM_val = 127; //(25% = 64; 50% = 127; 75% = 191; 100% = 255)
int 2PWM = 127; //(25% = 64; 50% = 127; 75% = 191; 100% = 255)


void setup(){
  pinMode(1INA, OUTPUT);
  pinMode(1INB, OUTPUT);
  pinMode(1PWM, OUTPUT);
  pinMode(2INA, OUTPUT);
  pinMode(2INB, OUTPUT);
  pinMode(2PWM, OUTPUT);
}

void loop(){
  digitalWrite(1INA, HIGH);  // sets to go forward?
  digitalWrite(1INB, LOW);
  
  digitalWrite(2INA, HIGH);  // sets to go forward?
  digitalWrite(2INB, LOW);
  
  analogWrite(1PWM, 1PWM_val);
  analogWrite(2PWM, 2PWM_val);
  
}
