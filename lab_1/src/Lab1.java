import TSim.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Lab1 {
  ArrayList<Critical> sections = new ArrayList(); 
  TSimInterface tsi = TSimInterface.getInstance();
  


  public Lab1(int speed1, int speed2) {
    tsi.setDebug(true);
    Critical down_left = new Critical(0);
    down_left.add(3,13);
    down_left.add(6,9);
    down_left.add(5,11);
    down_left.add(6,10);
    //down_left.add(1,10);
    Critical down_middle = new Critical(1);
    Critical down_right = new Critical(2);
    down_right.add(13,9);
    down_right.add(15,7);
    down_right.add(13,10);
    down_right.add(15,8);
    //down_right.add(19,8);
    Critical down = new Critical(3);
    down.add(10,11);
    down.add(10,13);
    Critical up_middle = new Critical(4);
    Critical cross = new Critical(5);
    cross.add(8,5);             // sensor up
    cross.add(6,7);             // sensor left
    cross.add(11,7);            // sensor right
    cross.add(10,8);             // sensor down
    Critical up = new Critical(6);
    up.add(10,3);
    up.add(10,5);

    sections.add(down_left);    // section 0
    sections.add(down_middle);  // section 1
    sections.add(down_right);   // section 2
    sections.add(down);         // section 3
    sections.add(up_middle);    // section 4
    sections.add(cross);        // section 5
    sections.add(up);           // section 6
    
    // create threads for each train
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

  private Critical isCritical(int x, int y){
    for (Critical c : sections) {
      for (Lab1.Critical.Sensor p : c.positions) {
        if(p.x == x && p.y == y){
          return c;
        }
      }
      
    }
    return null;
  }

  public void tryTrain(Critical c, Train t){
    if(c.tryAcquire()) {
      //if(sensor.isUp){switch.left or switch.right}

      return;
    }
    else {
      //find sensor
      //if()
      return;
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

  
  private class Critical extends Semaphore{
      private ArrayList<Sensor> positions = new ArrayList(); 
      private int id;

      private class Sensor {
        int x;
        int y;
        
        public Sensor(int x ,int y) {
          this.x = x;
          this.y = y;
        }
      }

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

  private class Train implements Runnable{
    private final int id;
    private final int speed;
    private SensorEvent sensor;
    private Critical current;
    private Critical rail;
    private boolean isAcquired = false;
    private boolean isDefault = true;
    //swtiches 15 9 right to zero cluster
    // switches 17 7 last before 4-crossing
    private boolean dir = true;

    public Train(int id, int speed){
      this.id = id;
      this.speed = speed;
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

    void check_track(Critical c, Train t){
      int index = getIndex(c);
      //TODO make exception
      
      t.setSpeed(0);
      switch(c.id) {
        case(0): section_zero(t,index); break; 
        case(2): section_two(t,index);  break;
        case(3): reverse_train(t);      break;
        case(5): section_cross(t,index);break;
        case(6): reverse_train(t);      break;

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
  
    public void section_zero (Train t, int sensor){
      int switch1_x = 3;
      int switch1_y = 11;
      int switch2_x = 4;
      int switch2_y = 9;
      Critical section = sections.get(0);

      if(!t.isAcquired) {
        changeSection(section);
        if(sensor == 2) {
          sections.get(3).release();
        }
        if(sensor == 0 || sensor == 2) {
          sections.get(3).release();
          if (sections.get(1).tryAcquire()){
            setSwitch(switch2_x, switch2_y, tsi.SWITCH_LEFT);
          }
          else {
            setSwitch(switch2_x, switch2_y, tsi.SWITCH_RIGHT);
          }
        }
        if(sensor == 1 || sensor == 3) {
          if (sensor == 1) {
            sections.get(1).release();
          }
          if(sections.get(3).tryAcquire()){
            setSwitch(switch1_x, switch1_y, tsi.SWITCH_LEFT);
          }
          else{
            setSwitch(switch1_x, switch1_y, tsi.SWITCH_RIGHT);
          }
        }
        t.isAcquired = true;
      }
      else {
        section.release();
        t.isAcquired = false;
      }
      switch(sensor){
        case(0): 
        setSwitch(switch1_x, switch1_y, tsi.SWITCH_RIGHT);   
        break;
        case(1): setSwitch(switch2_x, switch2_y, tsi.SWITCH_LEFT);    break;
        case(2): setSwitch(switch1_x, switch1_y, tsi.SWITCH_LEFT);    break;
        case(3): setSwitch(switch2_x, switch2_y, tsi.SWITCH_RIGHT);   break;
      }
      t.setSpeed(t.speed);
    }

    public void section_two (Train t, int sensor){
      System.out.println("Trying to catch");
      int switch3_x = 15;
      int switch3_y = 9;
      int switch4_x = 17;
      int switch4_y = 7;
      Critical section = sections.get(2);
      System.out.println(sensor);
      if(!t.isAcquired) {
          changeSection(section);
          if(sensor == 0) {
            sections.get(1).release();
          }
          if(sensor == 0 || sensor == 2) {
            if(sections.get(4).tryAcquire()) {
              setSwitch(switch4_x, switch4_y, tsi.SWITCH_RIGHT);
            }
            else {
              setSwitch(switch4_x, switch4_y, tsi.SWITCH_LEFT);
            }
          if(sensor == 1) {
            sections.get(4).release();
          }
          if(sensor == 1 || sensor == 3) {
            if(sections.get(1).tryAcquire()){
              setSwitch(switch3_x, switch3_y, tsi.SWITCH_RIGHT);
            }
            else {
              setSwitch(switch3_x, switch3_y, tsi.SWITCH_LEFT);
            }
            t.isAcquired = true;
          }
        }
          
      }
    
      else {
        section.release();
        t.isAcquired = false;
        }
        
        
      switch(sensor){
        case(0): setSwitch(switch3_x, switch3_y, tsi.SWITCH_RIGHT);  break;
        case(1): setSwitch(switch4_x, switch4_y, tsi.SWITCH_RIGHT);  break;
        case(2): setSwitch(switch3_x, switch3_y, tsi.SWITCH_LEFT);   break;
        case(3): setSwitch(switch4_x, switch4_y, tsi.SWITCH_LEFT);   break;
      }
      t.setSpeed(t.speed);
    }

    void section_cross(Train t, int sensor){
       this.setSpeed(t.speed); 
    }

    void reverse_train(Train t){
      try { 
          System.out.print(t.dir);
          if(!this.dir){
            t.setSpeed(0); 
            Thread.sleep(1+(2*t.speed));
            t.setSpeed(-t.speed);
          }
          else{
            this.setSpeed(speed);
          }
        }
        
        catch (InterruptedException e) {
          e.printStackTrace();
          System.exit(1);
        }
      
  
    }
    public void run(){
      while(true){
        try {
          this.sensor = tsi.getSensor(this.id);
          System.out.println(this.sensor.getXpos() + " " + this.sensor.getYpos());
          if(this.sensor.getStatus()==1) {
            Critical current = isCritical(sensor.getXpos(), sensor.getYpos());
            System.out.println("sensor from " + current.id + " triggered");
            check_track(current, this);
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
