
// left wheel motor
const int INA1 = 8;
const int INB1 = 9;
const int PWM1 = 10;
// right wheel motor
const int INA2 = 11;
const int INB2 = 12;
const int PWM2 = 13;
// left ping sensor
const int ping_left = 7;
const int pingservo_left = 7;
// right ping sensor
const int ping_right = 7;
const int pingservo_right = 7;

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
  // move left wheel
  move_wheel(true, true, 127);
  // move right wheel backwards
  move_wheel(false, false, 127);
  
  // read pings
  Serial.print("Left: ");
  Serial.print(ping_inches(ping_left, 0));
  Serial.print(", Right: ");
  Serial.print(ping_inches(ping_right, 0));
  Serial.println();
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
  // moves the given wheel at given direction at given speed (25% = 64; 50% = 127; 75% = 191; 100% = 255)
  // is non-blocking
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
  
  if (timeout){
      duration = pulseIn(ping_pin, HIGH, timeout);
  } else {
      duration = pulseIn(ping_pin, HIGH);
  }
  return duration;
}

long ping_inches(int ping_pin, unsigned long timout){
  return microseconds_to_inches(ping_duration(ping_pin, timeout));
}

long ping_centimeters(int ping_pin, unsigned long timout){
  return microseconds_to_centimeters(ping_duration(ping_pin, timeout));
}

