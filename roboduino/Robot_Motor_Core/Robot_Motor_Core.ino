#include <Servo.h>

// right wheel motor
const int INA1 = 8;
const int INB1 = 9;
const int PWM1 = 10;
// left wheel motor
const int INA2 = 11;
const int INB2 = 12;
const int PWM2 = 13;
// left ping sensor
const int ping_left = 22;
const int ping_servo_left = 6;   // PWM
// right ping sensor
const int ping_right = 24;
const int ping_servo_right = 7;  // PWM

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
  // debuging serial connection
  Serial.begin(9600);
}

void loop(){
  // detect pings
  
//  move_wheel(true, 64);
//  move_wheel(false, 64);
//  
//  delay(2000);
//  
//  move_wheel(true, -64);
//  move_wheel(false, -64);
//  
//  delay(2000);
  
  
  long left_inches = ping_inches(ping_left, 0);
  long right_inches = ping_inches(ping_right, 0);
  #define THRESHOLD 10
  #define HIGH_SPEED 127
  #define LOW_SPEED 40
  
  
  int left_speed = (left_inches < THRESHOLD) ? HIGH_SPEED : LOW_SPEED;
  int right_speed = (right_inches < THRESHOLD) ? HIGH_SPEED : LOW_SPEED;
  
  // don't run into a wall...
  if (left_speed == HIGH_SPEED && right_speed == HIGH_SPEED){
    left_speed = -(LOW_SPEED-10);
    right_speed = -(LOW_SPEED);
  }
  
  move_wheel(true, right_speed);
  move_wheel(false, left_speed);
  
  // read pings
  Serial.print("Left: ");
  Serial.print(ping_inches(ping_left, 0));
  Serial.print(", Right: ");
  Serial.print(ping_inches(ping_right, 0));
  Serial.println();
  
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

long microseconds_to_inches(long microseconds)
{
  // According to Parallax's datasheet for the PING))), there are
  // 73.746 microseconds per inch (i.e. sound travels at 1130 feet per
  // second).  This gives the distance travelled by the ping, outbound
  // and return, so we divide by 2 to get the distance of the obstacle.
  // See: http://www.parallax.com/dl/docs/prod/acc/28015-PING-v1.3.pdf
  return microseconds / 74 / 2;
}

long microseconds_to_centimeters(long microseconds)
{
  // The speed of sound is 340 m/s or 29 microseconds per centimeter.
  // The ping travels out and back, so to find the distance of the
  // object we take half of the distance travelled.
  return microseconds / 29 / 2;
}

// TODO: make this non-blocking
long ping_duration(int ping_pin, unsigned long timeout){
  // returns the duration it took to receive a pong after a ping
  
  // The PING))) is triggered by a HIGH pulse of 2 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  pinMode(ping_pin, OUTPUT);
  digitalWrite(ping_pin, LOW);
  delayMicroseconds(2);
  digitalWrite(ping_pin, HIGH);
  delayMicroseconds(5);
  digitalWrite(ping_pin, LOW);

  // The same pin is used to read the signal from the PING))): a HIGH
  // pulse whose duration is the time (in microseconds) from the sending
  // of the ping to the reception of its echo off of an object.
  pinMode(ping_pin, INPUT);
  
  int duration;
  if (timeout){
      duration = pulseIn(ping_pin, HIGH, timeout);
  } else {
      duration = pulseIn(ping_pin, HIGH);
  }
  return duration;
}

long ping_inches(int ping_pin, unsigned long timeout){
  return microseconds_to_inches(ping_duration(ping_pin, timeout));
}

long ping_centimeters(int ping_pin, unsigned long timeout){
  return microseconds_to_centimeters(ping_duration(ping_pin, timeout));
}
