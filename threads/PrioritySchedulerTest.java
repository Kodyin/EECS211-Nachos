 private static class PingTest implements Runnable  
    {  
        Lock a=null,b=null;  
        int name;  
        PingTest(Lock A,Lock B,int x)  
        {  
            a=A;b=B;name=x;  
        }  
        public void run() {  
            System.out.println("Thread "+name+" starts.");  
            if(b!=null)  
            {  
                System.out.println("Thread "+name+" waits for Lock b.");  
                b.acquire();  
                System.out.println("Thread "+name+" gets Lock b.");  
            }  
            if(a!=null)  
            {  
                System.out.println("Thread "+name+" waits for Lock a.");  
                a.acquire();  
                System.out.println("Thread "+name+" gets Lock a.");  
            }  
            KThread.yield();  
            boolean intStatus = Machine.interrupt().disable();  
            System.out.println("Thread "+name+" has priority "+ThreadedKernel.scheduler.getEffectivePriority()+".");  
            Machine.interrupt().restore(intStatus);  
            KThread.yield();  
            if(b!=null) b.release();  
            if(a!=null) a.release();  
            System.out.println("Thread "+name+" finishs.");  
              
        }  
    }  
      
    public static void selfTest()  
    {  
        Lock a=new Lock();  
        Lock b=new Lock();  
          
        Queue<KThread> qq=new LinkedList<KThread>();  
        for(int i=1;i<=5;i++)  
        {  
            KThread kk=new KThread(new PingTest(null,null,i));  
            qq.add(kk);  
            kk.setName("Thread-"+i).fork();  
        }  
        for(int i=6;i<=10;i++)  
        {  
            KThread kk=new KThread(new PingTest(a,null,i));  
            qq.add(kk);  
            kk.setName("Thread-"+i).fork();  
        }  
        for(int i=11;i<=15;i++)  
        {  
            KThread kk=new KThread(new PingTest(a,b,i));  
            qq.add(kk);  
            kk.setName("Thread-"+i).fork();  
        }  
        KThread.yield();  
        Iterator it=qq.iterator();  
        int pp=0;  
        while(it.hasNext())  
        {  
            boolean intStatus = Machine.interrupt().disable();  
            ThreadedKernel.scheduler.setPriority((KThread)it.next(),pp+1);  
            Machine.interrupt().restore(intStatus);  
            pp=(pp+1)%6+1;  
        }  
    }  
