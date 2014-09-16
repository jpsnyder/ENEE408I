
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
  // set up wheel motors
  pinMode(INA1, OUTPUT);
  pinMode(INB1, OUTPUT);
  pinMode(PWM1, OUTPUT);
  pinMode(INA2, OUTPUT);
  pinMode(INB2, OUTPUT);
  pinMode(PWM2, OUTPUT);
}

void loop(){
  // move left wheel
  move_wheel(true, true, 127);
  // move right wheel backwards
  move_wheel(false, false, 127);
//  digitalWrite(INA1, HIGH);  // sets to go forward?
//  digitalWrite(INB1, LOW);
//  
//  digitalWrite(INA2, HIGH);  // sets to go forward?
//  digitalWrite(INB2, LOW);
//  
//  analogWrite(PWM1, PWM1_val);
//  analogWrite(PWM2, PWM2_val);
  
}

void move_wheel(boolean left_wheel, boolean forward, int speed){
  int first = 0;
  int second = 0;
  int* pins = 0;
  
  if (left_wheel){
    pins = [INA1, INB1, PWM1];
  } else {
    pins = [INA2, INB2, PWM2]; 
  }
  
  if (forward){
    first = HIGH;
    second = LOW: 
  } else {
    first = LOW;
    second = HIGH; 
  }
  
  // TODO: offset one of the motors
  
  digitalWrite(pins[0], first);
  digitalWrite(pins[1], second);
  analogWrite(pins[3], speed);
}
