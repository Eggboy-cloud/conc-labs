import TSim.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Lab1 {
  ArrayList<Critical> sections = new ArrayList<Critical>(); 
  TSimInterface tsi = TSimInterface.getInstance();
  int SWITCH_LEFT = TSimInterface.SWITCH_LEFT;
  int SWITCH_RIGHT = TSimInterface.SWITCH_RIGHT;
  

  /**
   * Instantiates objects from the Class Critical which then adds sensors to each section
   * Adds all instances of Critical to the ArrayList sections
   * @param speed1
   *              speed of the first train
   * @param speed2
   *              speed of the second train
   */
  public Lab1(int speed1, int speed2) {
    Critical down = new Critical(0);
    down.add(13,11);
    down.add(13,13);
    Critical down_left = new Critical(1);
    down_left.add(5,13);
    down_left.add(8,9);
    down_left.add(7,11);
    down_left.add(8,10);
    Critical down_middle = new Critical(2);
    Critical down_right = new Critical(3);
    down_right.add(11,9);
    down_right.add(13,7);
    down_right.add(11,10);
    down_right.add(13,8);
    Critical up_middle = new Critical(4);
    Critical cross = new Critical(5);
    cross.add(10,5);            // sensor up
    cross.add(6,5);             // sensor left
    cross.add(12,7);            // sensor right
    cross.add(12,8);            // sensor down
    Critical up = new Critical(6);
    up.add(13,3);
    up.add(13,5);

    sections.add(down);         // section 0
    sections.add(down_left);    // section 1
    sections.add(down_middle);  // section 2
    sections.add(down_right);   // section 3
    sections.add(up_middle);    // section 4
    sections.add(cross);        // section 5
    sections.add(up);           // section 6
    
    // Create threads for each train
    Thread t_1 = new Thread(new Train(1,speed1));
    t_1.start();
    Thread t_2 = new Thread(new Train(2,speed2));
    t_2.start();
    try {
      tsi.setSpeed(1,speed1);
      tsi.setSpeed(2,speed2);
    }
    catch (CommandException e) {
      e.printStackTrace();    // or only e.getMessage() for the error
      System.exit(1);
    }
  }

  public void setSwitch(int x, int y, int dir) {
      try{
        tsi.setSwitch(x,y, dir);
      }

      catch (CommandException e){
        e.printStackTrace();
        System.exit(1);

      } 
  }

  public Critical isCritical(int x, int y){
    for (Critical c : sections) {
      for (Lab1.Critical.Sensor p : c.positions) {
        if(p.x == x && p.y == y){
          return c;
        }
      }
    }
    return null;
  }
  /**
   * The Class that holds sensors for each Critical section
   * It extends the Semaphore class since only a single train 
   * should have access to the section
   */
  public class Critical extends Semaphore{
      private ArrayList<Sensor> positions = new ArrayList<Sensor>(); 
      private int id;

      //Private Class for sensor
      private class Sensor {
        int x;
        int y;
        
        public Sensor(int x ,int y) {
          this.x = x;
          this.y = y;
        }
      }
      //Uses the semaphore constructor
      //And gives an id
      public Critical(int id){
        super(1,true);
        this.id = id;
      }

      public void add(int x, int y){
        this.positions.add(new Sensor(x,y));
      }

      public void remove(int i){
        this.positions.remove(i);
      }

      public int indexOf(Sensor sensor) {
        return positions.indexOf(sensor);
      }
    }
  //Train Class which can be run in a thread
  private class Train implements Runnable{
    private final int id;
    private int speed;
    private SensorEvent sensor;
    private boolean isAcquired = false;
    private boolean dir;

    public Train(int id, int speed){
      this.id = id;
      this.speed = speed;
      if(id == 1)
        this.dir = true;
      else
        this.dir = false;
    }

    void setSpeed(int speed){
        try {
          tsi.setSpeed(this.id, speed);
        }

        catch (CommandException e){
          e.printStackTrace();
          System.exit(1);

        }
    }
    int getIndex(Critical c) {
      for (Lab1.Critical.Sensor sensor : c.positions) {
        if(sensor.x == this.sensor.getXpos() && sensor.y == this.sensor.getYpos())
            return c.indexOf(sensor);
      }
      return -1;
    }
  /**
   * Chooses the correct code to run based on the id of the Critical section
   * @param c
   *         The critical section the train is in currently
   */
    void check_track(Critical c){
      int index = getIndex(c);
      
      this.setSpeed(0);
      switch(c.id) {
      case(0): section_zero(index); reverse_train(c.id);  break;
      case(1): section_one(index);                        break; 
      case(3): section_three(index);                      break; 
      case(5): section_cross(index);                      break;
      case(6): section_four(index); reverse_train(c.id);  break;
      }
    }

    public void changeSection(Critical c) {
      try {
        c.acquire(); 
      }
      catch (InterruptedException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    void section_zero(int sensor){
      Critical section = sections.get(0);
      if(sensor == 0){
          section.tryAcquire();
        }
    }
  
    void section_one (int sensor){
      int switch1_x = 3;
      int switch1_y = 11;
      int switch2_x = 4;
      int switch2_y = 9;
      Critical section = sections.get(1);

      if(!this.isAcquired) {
        changeSection(section);
        if(sensor == 2) {
          sections.get(0).release();
        }
        if(sensor == 0 || sensor == 2) {
          if (sections.get(2).tryAcquire()){
            setSwitch(switch2_x, switch2_y, SWITCH_LEFT);
          }
          else {
            setSwitch(switch2_x, switch2_y, SWITCH_RIGHT);
          }
        }
        if(sensor == 1 || sensor == 3) {
          if (sensor == 1) {
            sections.get(2).release();
          }
          if(sections.get(0).tryAcquire()){
            setSwitch(switch1_x, switch1_y, SWITCH_LEFT);
          }
          else{
            setSwitch(switch1_x, switch1_y, SWITCH_RIGHT);
          }
        }
        this.isAcquired = true;
      }
      else {
        section.release();
        this.isAcquired = false;
      }
      switch(sensor){
        case(0): setSwitch(switch1_x, switch1_y, SWITCH_RIGHT);   break;
        case(1): setSwitch(switch2_x, switch2_y, SWITCH_LEFT);    break;
        case(2): setSwitch(switch1_x, switch1_y, SWITCH_LEFT);    break;
        case(3): setSwitch(switch2_x, switch2_y, SWITCH_RIGHT);   break;
      }
      this.setSpeed(this.speed);
    }

    void section_three (int sensor){
      int switch3_x = 15;
      int switch3_y = 9;
      int switch4_x = 17;
      int switch4_y = 7;
      Critical section = sections.get(3);
      if(!this.isAcquired) {
          changeSection(section);
          if(sensor == 0) {
            sections.get(2).release();
          }
          if(sensor == 0 || sensor == 2) {
            if(sections.get(4).tryAcquire()) {
              setSwitch(switch4_x, switch4_y, SWITCH_RIGHT);
            }
            else {
              setSwitch(switch4_x, switch4_y, SWITCH_LEFT);
            }
          }
          if(sensor == 1) {
            sections.get(4).release();
          }
          if(sensor == 1 || sensor == 3) {
            if(sections.get(2).tryAcquire()){
              setSwitch(switch3_x, switch3_y, SWITCH_RIGHT);
            }
            else {
              setSwitch(switch3_x, switch3_y, SWITCH_LEFT);
            }
      
          }   
          this.isAcquired = true;
      }
    
      else {
        section.release();
        this.isAcquired = false;
        }
        
        
      switch(sensor){
        case(0): setSwitch(switch3_x, switch3_y, SWITCH_RIGHT);  break;
        case(1): setSwitch(switch4_x, switch4_y, SWITCH_RIGHT);  break;
        case(2): setSwitch(switch3_x, switch3_y, SWITCH_LEFT);   break;
        case(3): setSwitch(switch4_x, switch4_y, SWITCH_LEFT);   break;
      }
      this.setSpeed(this.speed);
    }

    void section_four(int sensor) {
      Critical section = sections.get(4);
      if(sensor == 0) {
        section.tryAcquire();
        this.setSpeed(this.speed);
      }
    }

    void section_cross(int sensor){
      Critical section = sections.get(5);
       if(!this.isAcquired) {
        changeSection(section);
        this.isAcquired = true; 
       }
      else {
        section.release();
        this.isAcquired = false;
      }
      this.setSpeed(this.speed);
      
    }

    void reverse_train(int id){
      try { 
          if(this.dir && id == 0){
            this.dir = false;
            this.setSpeed(0); 
            Thread.sleep(2000+(20*this.speed));
            this.speed = -this.speed;
            this.setSpeed(speed);
          }
          else if(!this.dir && id == 6) {
            this.dir = true;
            this.setSpeed(0); 
            Thread.sleep(2000+(20*this.speed));
            this.speed = -this.speed;
            this.setSpeed(this.speed);
          }
          else {
            this.setSpeed(speed);
          }
        }
        
        catch (InterruptedException e) {
          e.printStackTrace();
          System.exit(1);
        }
    }

    //Finds the sensor and checks which Critical section it belongs to
    public void run(){
      while(true){
        try {
          this.sensor = tsi.getSensor(this.id);
          if(this.sensor.getStatus()==1) {
            Critical current = isCritical(sensor.getXpos(), sensor.getYpos());
            check_track(current);
          }
        } 

        catch (CommandException e){
          e.printStackTrace();
          System.exit(1);

        }

        catch (InterruptedException e) {
          e.printStackTrace();
          System.exit(1);
        }

        }
      }
  }  
}