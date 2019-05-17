import java.util.ArrayList;

import com.oocourse.TimableOutput;
import com.oocourse.elevator3.PersonRequest;

public class Elevator implements Runnable {
    enum State {
        SLEEP,UP,DOWN
    }
    
    enum Direc {
        UP,DOWN,STILL
    }

    private long doorTime = 200;
    private long floorTime;
    private int maxLoad;
    private int load;
    
    private StopFlag sf;
    
    private String id;//id of the Elevator
    private State state;//state of the Elevator
    private Direc direc;
    private int curFloor;//current floor of the Elevator
    private int dstFloor;//destiny of the Elevator
    
    private Scheduler sc;
    private ArrayList<Person> personList;
    //up and down list buffer
    private ArrayList<PersonRequest> upList;
    private ArrayList<PersonRequest> downList;
    
    Elevator(Scheduler sc, StopFlag sf, String id,
            long floorTime, int maxLoad) {
        this.id = id;
        this.floorTime = floorTime;
        this.maxLoad = maxLoad;
        this.load = 0;
        this.state = State.SLEEP;
        this.direc = Direc.STILL;
        this.curFloor = 1;
        this.dstFloor = 1;       
        this.upList = new ArrayList<PersonRequest>();
        this.downList = new ArrayList<PersonRequest>();
        this.personList = new ArrayList<Person>();
        this.sf = sf;
        this.sc = sc;
    }
    
    public void addReq(PersonRequest pr) {
        if (pr.getFromFloor() > pr.getToFloor()) {
            addDownReq(pr);
        } else {
            addUpReq(pr);
        } 
    }
    
    public void addUpReq(PersonRequest pr) {
        insert(pr,upList);
    }
    
    public void addDownReq(PersonRequest pr) {
        insert(pr,downList);
    }
    
    private void insert(PersonRequest pr, ArrayList<PersonRequest> list) {
        synchronized (list) {  
            if (list.isEmpty()) {
                list.add(pr);
                return;
            } else {
                int i;
                for (i = 0; i < list.size(); i++) {
                    if (list.get(i).getFromFloor() < pr.getFromFloor()) {
                        continue;
                    }
                    break;
                }
                list.add(i,pr);
                return;
            }    
        }      
    }
    
    private void insert(Person p) {
        synchronized (personList) {  
            personList.add(p);
        }
    }
    
    private void updateStateAndDirec() {
        if (state == State.UP) {
            updateUp();
        } else {
            updateDown();
        }
    }

    private void updateUp() {
        if (direc == Direc.UP) {
            //man in elevator or not, shouldn't change state
            for (int i = 0; i < upList.size(); i++) {
                PersonRequest pr = upList.get(i);
                if (pr.getFromFloor() >= curFloor &&
                        pr.getToFloor() > dstFloor) {
                    dstFloor = pr.getToFloor();
                }
            }
            if (!downList.isEmpty()) {
                PersonRequest pr = downList.get(downList.size() - 1);
                if (pr.getFromFloor() > dstFloor) {
                    dstFloor = pr.getFromFloor();
                }
            }
            if (dstFloor == curFloor) {
                state = State.DOWN;
                direc = Direc.STILL;
                if (!downList.isEmpty()) {
                    direc = Direc.DOWN;
                    dstFloor = getSmall(getLowToDown(), getLowFromUp());
                }
            }
        } else if (direc == Direc.DOWN) {
            //the man is not in elevator, can change state to down
            if (!downList.isEmpty() && 
                    downList.get(0).getFromFloor() <= curFloor) {
                state = State.DOWN;
                dstFloor = getLowFromUp();
                for (int i = 0; i < downList.size(); i++) {
                    PersonRequest pr = downList.get(i);
                    if (pr.getFromFloor() > curFloor) {
                        break;                           
                    }
                    if (pr.getToFloor() < dstFloor) {
                        dstFloor = pr.getToFloor();
                    }                       
                }                
            } else {
                dstFloor = getLowFromUp();
                if (dstFloor == curFloor) {
                    direc = Direc.UP;
                    dstFloor = getBig(getHighToUp(), getHighFromDown());
                }
            }
        }
    }
    
    private void updateDown() {
        if (direc == Direc.DOWN) {
            //man in elevator or not, shouldn't change state
            for (int i = 0; i < downList.size(); i++) {
                PersonRequest pr = downList.get(i);
                if (pr.getFromFloor() <= curFloor &&
                        pr.getToFloor() < dstFloor) {
                    dstFloor = pr.getToFloor();
                }
            }
            if (!upList.isEmpty()) {
                PersonRequest pr = upList.get(0);
                if (pr.getFromFloor() < dstFloor) {
                    dstFloor = pr.getFromFloor();
                }
            }
            if (dstFloor == curFloor) {
                state = State.UP;
                direc = Direc.STILL;
                if (!upList.isEmpty()) {
                    direc = Direc.UP;
                    dstFloor = getBig(getHighToUp(), getHighFromDown());
                }
            }
        } else if (direc == Direc.UP) {
            //the man is not in elevator, can change state to up
            if (!upList.isEmpty() && 
                    upList.get(upList.size() - 1).getFromFloor() >= curFloor) {
                state = State.UP;
                dstFloor = getHighFromDown();
                for (int i = upList.size() - 1; i >= 0; i--) {
                    PersonRequest pr = upList.get(i);
                    if (pr.getFromFloor() < curFloor) {
                        break;                           
                    }
                    if (pr.getToFloor() > dstFloor) {
                        dstFloor = pr.getToFloor();
                    }                       
                }
            } else {
                dstFloor = getHighFromDown();
                if (dstFloor == curFloor) {
                    direc = Direc.DOWN;
                    dstFloor = getSmall(getLowFromUp(), getLowToDown());
                }
            }
        }
    }
    
    private int getLowFromUp() {
        int value = 15;
        for (int i = 0; i < upList.size(); i++) {
            if (upList.get(i).getFromFloor() < value) {
                value = upList.get(i).getFromFloor();
            }
        }
        return value;
    }
    
    private int getHighToUp() {
        int value = 1;
        for (int i = 0; i < upList.size(); i++) {
            if (upList.get(i).getToFloor() > value) {
                value = upList.get(i).getToFloor();
            }
        }
        return value;
    }
    
    private int getHighFromDown() {
        int value = 1;
        for (int i = 0; i < downList.size(); i++) {
            if (downList.get(i).getFromFloor() > value) {
                value = downList.get(i).getFromFloor();
            }
        }
        return value;
    }
    
    private int getLowToDown() {
        int value = 15;
        for (int i = 0; i < downList.size(); i++) {
            if (downList.get(i).getToFloor() < value) {
                value = downList.get(i).getToFloor();
            }
        }
        return value;
    }
    
    private void changeState() {
        state = State.SLEEP;
        direc = Direc.STILL;
    }
    
    private void tryOpenDoor() throws Exception {
        if (state == State.UP) {
            if (direc == Direc.UP) {
                for (int i = 0; i < personList.size(); i++) {
                    if (personList.get(i).getDst() == curFloor) {
                        openDoor(upList);
                        return;
                    }
                }
                if (load == maxLoad) {
                    return;
                }
                for (int i = 0; i < upList.size(); i++) {
                    if (upList.get(i).getFromFloor() == curFloor) {
                        openDoor(upList);
                        return;
                    }
                }                
            } else {
                for (int i = 0; i < personList.size(); i++) {
                    if (personList.get(i).getDst() == curFloor) {
                        openDoor();
                        return;
                    }
                }
            }            
        } else {
            if (direc == Direc.DOWN) {
                for (int i = 0; i < personList.size(); i++) {
                    if (personList.get(i).getDst() == curFloor) {
                        openDoor(downList);
                        return;
                    }
                }
                if (load == maxLoad) {
                    return;
                }
                for (int i = 0; i < downList.size(); i++) {
                    if (downList.get(i).getFromFloor() == curFloor) {
                        openDoor(downList);
                        return;
                    }
                }
            } else {
                for (int i = 0; i < personList.size(); i++) {
                    if (personList.get(i).getDst() == curFloor) {
                        openDoor();
                        return;
                    }
                }
            }            
        }
    }
    
    private void openDoor() throws Exception {
        TimableOutput.println(String.format("OPEN-%d" + tail(),curFloor));
        Thread.sleep(doorTime);
        
        synchronized (personList) {
            for (int i = 0; i < personList.size(); i++) {
                int dst = personList.get(i).getDst();
                if (dst == curFloor) {
                    int pid = personList.get(i).getId();
                    TimableOutput.println(
                            String.format("OUT-%d-%d" + tail(),pid,curFloor));
                    personList.remove(i);
                    i--;
                    load--;
                    sc.awakeP(pid);
                }
            }
        }
        if (personList.isEmpty()) {
            changeState();
        }
                
        Thread.sleep(doorTime);
        TimableOutput.println(String.format("CLOSE-%d" + tail(),curFloor));
    }
    
    private void openDoor(ArrayList<PersonRequest> list) throws Exception {
        TimableOutput.println(String.format("OPEN-%d" + tail(),curFloor));
        Thread.sleep(doorTime);
        
        synchronized (personList) {
            for (int i = 0; i < personList.size(); i++) {
                int dst = personList.get(i).getDst();
                if (dst == curFloor) {
                    int pid = personList.get(i).getId();
                    TimableOutput.println(
                            String.format("OUT-%d-%d" + tail(),pid,curFloor));
                    personList.remove(i);
                    i--;
                    load--;
                    sc.awakeP(pid);
                }
            }
        }
        
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                if (load == maxLoad) {
                    break;
                }
                PersonRequest pr = list.get(i);
                if (pr.getFromFloor() == curFloor) {
                    int pid = pr.getPersonId();
                    TimableOutput.println(
                            String.format("IN-%d-%d" + tail(),pid,curFloor));
                    insert(new Person(pr));
                    refreshDstFloor(pr);
                    list.remove(i);
                    i--;
                    load++;
                }
            }
        }
        if (personList.isEmpty()) {
            changeState();
        }
        
        Thread.sleep(doorTime);
        TimableOutput.println(String.format("CLOSE-%d" + tail(),curFloor));
    }

    private void refreshDstFloor(PersonRequest pr) {
        if (pr.getToFloor() > dstFloor && state == State.UP) {
            dstFloor = pr.getToFloor();
        } else if (pr.getToFloor() < dstFloor && state == State.DOWN) {
            dstFloor = pr.getToFloor();
        }
    }
    
    private boolean trySleep() {
        if (state == State.SLEEP && direc == Direc.STILL) {
            return true;
        }
        if (personList.isEmpty() && upList.isEmpty() && downList.isEmpty()) {
            state = State.SLEEP;
            direc = Direc.STILL;
            return true;
        }
        return false;
    }
    
    private void moveUp() {
        curFloor++;
        if (curFloor == 0) {
            curFloor++;
        }
    }
    
    private void moveDown() {
        curFloor--;
        if (curFloor == 0) {
            curFloor--;
        }
    }
    
    private void outputArrive() {
        TimableOutput.println(String.format("ARRIVE-%d" + tail(),curFloor));
    }
    
    private int getBig(int a, int b) {
        if (a > b) {
            return a;
        } else {
            return b;
        }        
    }
    
    private int getSmall(int a, int b) {
        if (a < b) {
            return a;
        } else {
            return b;
        }
    }
    
    private String tail() {
        return "-" + id;
    }
    
    @Override
    public void run() {
        try {
            outer : while (true) {
                while (state == State.SLEEP) {
                    if (!upList.isEmpty()) {
                        state = State.UP;
                        dstFloor = getLowFromUp();
                        if (dstFloor < curFloor) {
                            direc = Direc.DOWN;
                        } else {
                            direc = Direc.UP;
                            dstFloor = getHighToUp();
                        }
                        break;
                    }
                    if (!downList.isEmpty()) {
                        state = State.DOWN;
                        dstFloor = getHighFromDown();
                        if (dstFloor > curFloor) {
                            direc = Direc.UP;
                        } else {
                            direc = Direc.DOWN;
                            dstFloor = getLowToDown();
                        }
                        break;
                    }
                    if (sf.getFlag() && upList.isEmpty() && 
                            downList.isEmpty() && personList.isEmpty()) {
                        break outer;
                    }
                    synchronized (this) {
                        this.wait();
                    }
                }
                while (state != State.SLEEP) {
                    updateStateAndDirec();
                    tryOpenDoor();
                    if (trySleep()) {
                        break;
                    }
                    if (direc == Direc.UP) {
                        Thread.sleep(floorTime);
                        moveUp();
                        outputArrive();
                    } else if (direc == Direc.DOWN) {
                        Thread.sleep(floorTime);
                        moveDown();
                        outputArrive();
                    } else {                        
                        state = State.SLEEP;                        
                    }
                    Thread.yield();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}
