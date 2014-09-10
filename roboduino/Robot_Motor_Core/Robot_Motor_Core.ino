/* Motor Core

  This code for the Arduino Robot's motor board
  is the stock firmware. program the motor board with 
  this sketch whenever you want to return the motor
  board to its default state.
  
*/

//#include <ArduinoRobotMotorBoard.h>
int INA1 = 8;
int INB1 = 9;
int PWM1 = 10;
int INA2 = 11;
int INB2 = 12;
int PWM2 = 13;

int PWM1_val = 127; //(25% = 64; 50% = 127; 75% = 191; 100% = 255)
int PWM2_val = 127; //(25% = 64; 50% = 127; 75% = 191; 100% = 255)


void setup(){
  pinMode(INA1, OUTPUT);
  pinMode(INB1, OUTPUT);
  pinMode(PWM1, OUTPUT);
  pinMode(INA2, OUTPUT);
  pinMode(INB2, OUTPUT);
  pinMode(PWM2, OUTPUT);
}

void loop(){
  digitalWrite(INA1, HIGH);  // sets to go forward?
  digitalWrite(INB1, LOW);
  
  digitalWrite(INA2, HIGH);  // sets to go forward?
  digitalWrite(INB2, LOW);
  
  analogWrite(PWM1, PWM1_val);
  analogWrite(PWM2, PWM2_val);
  
}
