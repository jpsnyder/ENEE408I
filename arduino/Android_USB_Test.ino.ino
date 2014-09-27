#include <Servo.h>

// right wheel motor
const int INA1 = 8;
const int INB1 = 9;
const int PWM1 = 10;
// left wheel motor
const int INA2 = 11;
const int INB2 = 12;
const int PWM2 = 13;
// right ping sensor
const int ping_right = 22;
const int ping_servo_right = 6;   // PWM
// left ping sensor
const int ping_left = 24;
const int ping_servo_left = 7;  // PWM

int PWM1_val = 127; //(25% = 64; 50% = 127; 75% = 191; 100% = 255)
int PWM2_val = 127; //(25% = 64; 50% = 127; 75% = 191; 100% = 255)

#define HIGH_SPEED 160
#define LOW_SPEED 80
#define STOP 0
#define OFFSET 6

void setup() {
  // set up wheel motors
  pinMode(INA1, OUTPUT);
  pinMode(INB1, OUTPUT);
  pinMode(PWM1, OUTPUT);
  pinMode(INA2, OUTPUT);
  pinMode(INB2, OUTPUT);
  pinMode(PWM2, OUTPUT);
  SerialUSB.begin(115200); // speed is irrelevant for native port
}

void loop() {
    int left_speed = STOP;
    int right_speed = STOP;
  
  char c = SerialUSB.read();
  switch(c){
    case 'F': case 'f':
    left_speed = HIGH_SPEED + OFFSET;
    right_speed = HIGH_SPEED;
    break;
    case 'B': case 'b':
    left_speed = -HIGH_SPEED + OFFSET;
    right_speed = -HIGH_SPEED;
    break;
    case 'R': case 'r':
    left_speed = HIGH_SPEED + OFFSET;
    right_speed = STOP;
    break;
    case 'L': case 'l':
    left_speed = STOP;
    right_speed = HIGH_SPEED;
    break;
    case 'S': case 's':
    left_speed = STOP;
    right_speed = STOP;
    break;
    move_wheel(true, right_speed);
    move_wheel(false, left_speed);
  }
  
}

void move_wheel(boolean right_wheel, int wheel_speed){
  // moves the given wheel at given direction at given speed (25% = 64; 50% = 127; 75% = 191; 100% = 255)
  // is non-blocking
  int first = 0;
  int second = 0;
  int pins[3];
  
  if (right_wheel){
    pins[0] = INA1;
    pins[1] = INB1;
    pins[2] = PWM1;
    if (wheel_speed > 0){
      first = LOW;
      second = HIGH;
    } else if (wheel_speed < 0){
      first = HIGH;
      second = LOW; 
    } else {
      first = LOW;
      second = LOW;
    }
  } else {
    pins[0] = INA2;
    pins[1] = INB2;
    pins[2] = PWM2;
    if (wheel_speed > 0){
      first = HIGH;
      second = LOW;
    } else if (wheel_speed < 0){
      first = LOW;
      second = HIGH; 
    } else {
      first = LOW;
      second = LOW; 
    }
  }
  
//  if (forward){
//    first = LOW;
//    second = HIGH;
//  } else {
//    first = HIGH;
//    second = LOW; 
//  }
  
  // TODO: offset one of the motors
  digitalWrite(pins[0], first);
  digitalWrite(pins[1], second);
  analogWrite(pins[2], abs(wheel_speed));
}
