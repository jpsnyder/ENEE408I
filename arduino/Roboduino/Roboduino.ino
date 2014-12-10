#include <Servo.h>
#include <stdlib.h>
#define LEFT 0
#define RIGHT 1
#define WALL_THRESHOLD 30   // wall Threshold in inches
#define HIGH_SPEED 150
#define WALL_SPEED 120
#define LOW_SPEED 80
#define STOP 0
#define LEFT_OFFSET 6  // extra speed to compensate for left motor going slower
#define ONE_ROTATION 3200  // number of ticks per rotation
#define PI 3.14159265359
#define SERVO_LEFT_DEFAULT 20
#define SERVO_RIGHT_DEFAULT 160

// right wheel encoder
const int encoderRPinA = 2;
const int encoderRPinB = 3;
volatile unsigned long encoderRPos;
// left wheel encoder
const int encoderLPinA = 4;
const int encoderLPinB = 5;
volatile unsigned long encoderLPos;
// left wheel motor
const int INA1 = 8;
const int INB1 = 9;
const int PWM1 = 10;
// right wheel motor
const int INA2 = 11;
const int INB2 = 12;
const int PWM2 = 13;
// right ping sensor
const int ping_right = 22;
Servo servo_right;
const int servo_right_pin = 6;   // PWM
// left ping sensor
const int ping_left = 24;
Servo servo_left;
const int servo_left_pin = 7;  // PWM


void setup() {
  // servo setup
  servo_right.attach(servo_right_pin);
  servo_left.attach(servo_left_pin);
  servo_right.write(SERVO_RIGHT_DEFAULT);
  servo_left.write(SERVO_LEFT_DEFAULT);
  //left encoder setup
  pinMode(encoderLPinA, INPUT);
  pinMode(encoderLPinB, INPUT);
  attachInterrupt(encoderLPinA, doEncoderLA, CHANGE);
  attachInterrupt(encoderLPinB, doEncoderLB, CHANGE);
  encoderLPos = 0;
  //right encoder setup
  pinMode(encoderRPinA, INPUT);
  pinMode(encoderRPinB, INPUT);
  attachInterrupt(encoderRPinA, doEncoderRA, CHANGE);
  attachInterrupt(encoderRPinB, doEncoderRB, CHANGE);
  encoderRPos = 0;

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

void loop() {

  //wall_follow(120, RIGHT);
  //wall_follow(120, LEFT);
  // follow instructions given by arduino
  char msg[6];
  if (Serial.readBytes(msg, 1 + sizeof(int))) {
     switch ((char) msg[0]) {
       case 'D':
         move_robot(LOW_SPEED, atoi((const char*)(msg + 1)));
         break;
       case 'R':
         rotate_robot(LOW_SPEED, atoi((const char*)(msg + 1)));
         break;
       case '<':
         wall_follow(WALL_SPEED, LEFT);
         break;
       case '>':
         wall_follow(WALL_SPEED, RIGHT);
         break;
     }
  }
  
  
}

void rotate_robot(int wheel_speed, float angle) {
  //counter-clockwise is positive
  int wheel_speedL = angle > 0 ? -wheel_speed - LEFT_OFFSET : wheel_speed + LEFT_OFFSET;
  int wheel_speedR = angle > 0 ? wheel_speed : -wheel_speed;

  long target = 15 * abs(angle); // 3600 * (arclength / wheel circumference)
  encoderLPos = 0;
  encoderRPos = 0;
  while (wheel_speedL != STOP || wheel_speedR != STOP) {
    if (encoderLPos >= target)
      wheel_speedL = STOP;
    if (encoderRPos >= target)
      wheel_speedR = STOP;
    move_wheel(LEFT, wheel_speedL);
    move_wheel(RIGHT, wheel_speedR);

    // determine if android called stop
//    char msg;
//    if (Serial.readBytes(&msg, 1) && msg == 'S') {
//      move_wheel(LEFT, STOP);
//      move_wheel(RIGHT, STOP);
//      Serial.write('S'); // notify we stopped instead of completed
//      return;
//    }
  }
  Serial.write('A');  // send acknowledgement
}

void move_robot(int wheel_speed, float rotations) {
  int wheel_speedL = wheel_speed + LEFT_OFFSET;
  int wheel_speedR = wheel_speed;
  encoderLPos = 0; // reset wheel encoders
  encoderRPos = 0;
  int thresh = 2;
  //unsigned long start_posL = encoderLPos;
  //unsigned long start_posR = encoderRPos;
  //unsigned long start_time = millis();
  float speedL, speedR;
  long left_inches = 17, right_inches = 17;
  while ((((encoderLPos / ONE_ROTATION) <= rotations) && ((encoderRPos / ONE_ROTATION) <= rotations))) {
    if (encoderLPos / ONE_ROTATION <= rotations)
      move_wheel(LEFT, wheel_speedL);
    else move_wheel(LEFT, STOP);
    if (encoderRPos / ONE_ROTATION <= rotations)
      move_wheel(RIGHT, wheel_speedR);
    else move_wheel(RIGHT, STOP);
    long startL = encoderLPos;
    long startR = encoderRPos;
    delay(50);
    //time = millis();
    long diff = (long)((encoderLPos - startL) - (encoderRPos - startR));
    //    Serial.print(" Dif: ");
    //    Serial.println(diff);
    if (abs(diff) < thresh)
      continue;
    else if (diff > 0) {
      wheel_speedL -= 1;
      //wheel_speedR += 1;
    }
    else {
      wheel_speedL += 1;
      //wheel_speedR -= 1;
    }

    // determine if android called stop or ping sensors detected wall too close
    char msg;
    long left_inches = ping_inches(ping_left, 0);
    long right_inches = ping_inches(ping_right, 0);
    if (left_inches < 24 || right_inches < 24 || (Serial.readBytes(&msg, 1) && msg == 'S')){
      move_wheel(LEFT, STOP);
      move_wheel(RIGHT, STOP);
      Serial.write('S'); // notify we stopped instead of completed
      return;
    }
  }
  move_wheel(RIGHT, STOP);
  move_wheel(LEFT, STOP);
  Serial.write('A');  // send acknowledgement
}

void move_wheel(boolean right_wheel, int wheel_speed) {
  // moves the given wheel at given direction at given speed (25% = 64; 50% = 127; 75% = 191; 100% = 255)
  // is non-blocking
  int first = 0;
  int second = 0;
  int pins[3];

  // Set INA, INB
  if (!right_wheel) {
    pins[0] = INA1;
    pins[1] = INB1;
    pins[2] = PWM1;
    if (wheel_speed > 0) {
      first = HIGH;
      second = LOW;
    } else if (wheel_speed < 0) {
      first = LOW;
      second = HIGH;
    } else {
      first = LOW;
      second = LOW;
    }
  } else {
    pins[0] = INA2;
    pins[1] = INB2;
    pins[2] = PWM2;
    if (wheel_speed > 0) {
      first = LOW;
      second = HIGH;
    } else if (wheel_speed < 0) {
      first = HIGH;
      second = LOW;
    } else {
      first = LOW;
      second = LOW;
    }
  }


  // TODO: offset one of the motors
  digitalWrite(pins[0], first);
  digitalWrite(pins[1], second);
  analogWrite(pins[2], abs(wheel_speed));
}

long inches_to_microseconds(long inches) {
  return inches * 2 * 74;
}

long centimeters_to_microseconds(long centimeters) {
  return centimeters * 2 * 29;
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
long ping_duration(int ping_pin, unsigned long timeout) {
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
  if (timeout) {
    duration = pulseIn(ping_pin, HIGH, timeout);
  } else {
    duration = pulseIn(ping_pin, HIGH);
  }
  return duration;
}

void wall_follow(int spd, int dir) {

  char msg;
  int left_speed = spd + LEFT_OFFSET;
  int right_speed = spd;
  int margin = 0;
  int wall_ping = (dir == LEFT) ? ping_left : ping_right;
  int check_ping = (dir == LEFT) ? ping_right : ping_left;
  int servo_right_angle, servo_left_angle;
  if (dir == LEFT) {
    servo_right_angle = 160;
    servo_left_angle = 90;
  } else {
    servo_right_angle = 90;
    servo_left_angle = 20;
  }
  servo_right.write(servo_right_angle);
  servo_left.write(servo_left_angle);
  delay(1000);
  long max_wall_thresh = 50;
  long wall_dist, check_dist, prev_wall_dist;
  while (1) {
    //float kp = 0.6, ki = 0.05, kd = 0.1;
    float kp = 0.7, ki = 0.05, kd = 0.2, output, dt;
    float previous_error = 0;
    float integral = 0;
    float error, derivative;
    float wall_thresh = 15;
    unsigned long time = millis();

    check_dist = ping_inches(check_ping, 0);
    while (check_dist > 16) { // begin wall following adventure
      delay(80);
      prev_wall_dist = wall_dist;
      wall_dist = ping_inches(wall_ping, 0);
      wall_dist = wall_dist ? wall_dist : prev_wall_dist;
      check_dist = ping_inches(check_ping, 0);
      dt = ((float)(millis() - time)) / 1000;
      time = millis();
      error =  (wall_thresh - wall_dist);
      error = error > 8 ? error * 3 : error;
      integral = integral + error * dt;
      derivative = (error - previous_error) / dt;
      output = min(max(kp * error + ki * integral + kd * derivative, -130), 150);
      previous_error = error;
      
      if (dir == LEFT) {
        move_wheel(LEFT, spd + output + LEFT_OFFSET);
        move_wheel(RIGHT, spd - output);
      } else {
        move_wheel(LEFT, spd - output + LEFT_OFFSET);
        move_wheel(RIGHT, spd + output);
      }
      




      /*
      Serial.print("Output = ");
      Serial.println(output);
          Serial.print("Integral = ");
      Serial.println(integral);
          Serial.print("derivative = ");
      Serial.println(derivative);
              Serial.print("error = ");
      Serial.println(error);
      */
      //Serial.print("wall_dist = ");
      //Serial.println(wall_dist);
    } // end wall following adventure
    
    move_wheel(LEFT, STOP);
    move_wheel(RIGHT, STOP);
    if ((Serial.readBytes(&msg, 1) && msg == 'S')){
        move_wheel(LEFT, STOP);
        move_wheel(RIGHT, STOP);
        Serial.write('S'); // notify we stopped instead of completed
        return;
      }
    rotate_robot(LOW_SPEED, (dir == LEFT) ? -90 : 90);
  }
}










/*
while ( 1 ) {
  // align servos
  servo_right.write(servo_right_angle);
  servo_left.write(servo_left_angle);

  // check ping distances
  wall_dist = ping_inches(wall_ping, 0);
  check_dist = ping_inches(check_ping, 0);

  // robot is too far away from the wall, turn towards the wall.
  if ( max_wall_thresh > wall_dist > (wall_thresh + margin)) {
    // wall is right next to the robot, continue moving straight
  } else if ( wall_dist > max_wall_thresh) {
    // wall has disapear, carve the corner
    if (dir == LEFT){
      left_speed += 5;
      right_speed = 0;

    } else {
      left_speed = 0;
      right_speed += 5;
    }

  }

//      if (dir == LEFT) {
//        left_speed -= 5;
//        right_speed += 5;
//      } else {
//        left_speed += 5;
//        right_speed -= 5;
//      }
  } else if ( wall_dist < (wall_thresh - margin)) {
    if (dir == LEFT) {
      left_speed += 5;
      right_speed = 0;
    } else {
      left_speed = 0;
      right_speed += 5;
    }
  }


  // determine if a corner
  // if check_dist < 10 turn oposite of wall else if wall_dist > big threshold, rotate towards the wall

//    if (check_dist < 10) {
//      if (dir == LEFT){
//        // rotate 90 degrees clock-wise
//        rotate_robot(LOW_SPEED, 90);
//      } else {
//        // rotate 90 degrees counter clock-wise
//        rotate_robot(LOW_SPEED, -90);
//      }
//      // reset speeds
//      left_speed = spd + LEFT_OFFSET;
//      right_speed = spd;
//    // check if wall_ping is very far from the wall, we hit a corner
//    } else if (wall_ping > 24){
//      // clear away from the the wall
//      move_robot(LOW_SPEED, 2);
//      // rotate towards the previously existing wall
//      if (dir == LEFT){
//        rotate_robot(LOW_SPEED, -90);
//      } else {
//        rotate_robot(LOW_SPEED, 90);
//      }
//      // set up position
//      move_robot(LOW_SPEED, 2);
//      left_speed = spd + LEFT_OFFSET;
//      right_speed = spd;
//    }


  move_wheel(LEFT, left_speed);
  move_wheel(RIGHT, right_speed);

  if ((Serial.readBytes(&msg, 1) && msg == 'S') || left_speed == STOP || right_speed == STOP) {
    servo_right.write(130);
    servo_left.write(30);
    move_wheel(LEFT, STOP);
    move_wheel(RIGHT, STOP);
    Serial.write('S'); // notify we stopped instead of completed
    return;
  }
}
}
*/


long ping_inches(int ping_pin, unsigned long timeout) {
  return microseconds_to_inches(ping_duration(ping_pin, inches_to_microseconds(timeout)));
}

long ping_centimeters(int ping_pin, unsigned long timeout) {
  return microseconds_to_centimeters(ping_duration(ping_pin, centimeters_to_microseconds(timeout)));
}


// Wheel Encoder Interrupt Callbacks ========

void doEncoderLA() {
  encoderLPos++;
}

void doEncoderLB() {
  encoderLPos++;
}

void doEncoderRA() {
  encoderRPos++;
}

void doEncoderRB() {
  encoderRPos++;
}


//  long left_inches = ping_inches(ping_left, /*THRESHOLD + 5*/0);
//  long right_inches = ping_inches(ping_right, /*THRESHOLD + 5*/0);
//
//
//
//  int left_speed = ((right_inches < THRESHOLD) ? STOP : HIGH_SPEED) + LEFT_OFFSET;
//  int right_speed = (left_inches < THRESHOLD) ? STOP : HIGH_SPEED;
//
//  // don't run into a wall...
//  if (left_speed == STOP && right_speed == STOP){
//    left_speed = -(LOW_SPEED);
//    right_speed = -(LOW_SPEED);
//  }
// read pings
//  Serial.print("Left: ");
//  Serial.print(ping_inches(ping_left, 0));
//  Serial.print(", Right: ");
//  Serial.print(ping_inches(ping_right, 0));
//  Serial.println();

