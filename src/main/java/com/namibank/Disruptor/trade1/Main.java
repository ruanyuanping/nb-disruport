package com.namibank.Disruptor.trade1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import com.namibank.Disruptor.trade.Trade;

/**
 * 在复杂场景下使用RingBuffer（希望P1生产的数据给C1、C2并行执行，最后C1、C2执行结束后C3执行）
 * 一个生产者多个消费者
 * @author Administrator
 *
 */
public class Main {  
    public static void main(String[] args) throws InterruptedException {  
       
    	//当前系统时间
    	long beginTime=System.currentTimeMillis();  
    	//缓冲区大小
        int bufferSize=1024;  
        
        //8个线程
        ExecutorService executor=Executors.newFixedThreadPool(8);  

        /**
		//BlockingWaitStrategy 是最低效的策略，但其对CPU的消耗最小并且在各种不同部署环境中能提供更加一致的性能表现
		WaitStrategy BLOCKING_WAIT = new BlockingWaitStrategy();
		//SleepingWaitStrategy 的性能表现跟BlockingWaitStrategy差不多，对CPU的消耗也类似，但其对生产者线程的影响最小，适合用于异步日志类似的场景
		WaitStrategy SLEEPING_WAIT = new SleepingWaitStrategy();
		//YieldingWaitStrategy 的性能是最好的，适合用于低延迟的系统。在要求极高性能且事件处理线数小于CPU逻辑核心数的场景中，推荐使用此策略；例如，CPU开启超线程的特性
		WaitStrategy YIELDING_WAIT = new YieldingWaitStrategy();
		*/
        Disruptor<Trade> disruptor = new Disruptor<Trade>(new EventFactory<Trade>() {  
            @Override  
            public Trade newInstance() {  
                return new Trade();  
            }  
        }, bufferSize, executor, ProducerType.SINGLE, new BusySpinWaitStrategy());  
        
        //菱形操作
        /**
        //使用disruptor创建消费者组C1,C2  
        EventHandlerGroup<Trade> handlerGroup = 
        		disruptor.handleEventsWith(new Handler1(), new Handler2());
        //声明在C1,C2完事之后执行JMS消息发送操作 也就是流程走到C3 
        handlerGroup.then(new Handler3());
        */
        
        //顺序操作
        
        disruptor.handleEventsWith(new Handler1()).
        	handleEventsWith(new Handler2()).
        	handleEventsWith(new Handler3());
        
        
        //六边形操作. 
        /*
        Handler1 h1 = new Handler1();
        Handler2 h2 = new Handler2();
        Handler3 h3 = new Handler3();
        Handler4 h4 = new Handler4();
        Handler5 h5 = new Handler5();
        disruptor.handleEventsWith(h1, h2);
        disruptor.after(h1).handleEventsWith(h4);
        disruptor.after(h2).handleEventsWith(h5);
        disruptor.after(h4, h5).handleEventsWith(h3);
        */
        
        
        
        disruptor.start();//启动  
        CountDownLatch latch=new CountDownLatch(1);  
        //生产者准备  
        executor.submit(new TradePublisher(latch, disruptor));
        
        latch.await();//等待生产者完事. 
        
        disruptor.shutdown();  
        executor.shutdown();  
        System.out.println("总耗时:"+(System.currentTimeMillis()-beginTime));  
    }  
}  