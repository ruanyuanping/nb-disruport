package com.namibank.Disruptor.trade;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.WorkerPool;
/**
 * WorkProcessor 消息处理器：确保每个sequence只被一个processor消费，在同一个WorkPool中的处理多个WorkProcessor不会消费同样的sequence。
 * @author Administrator
 *
 */
public class Main2 {  
    public static void main(String[] args) throws InterruptedException {  
    	//缓冲区大小
        int BUFFER_SIZE = 1024;  
        //线程数
        int THREAD_NUMBERS = 4;  
        
        EventFactory<Trade> eventFactory = new EventFactory<Trade>() {  
            public Trade newInstance() {  
                return new Trade();  
            }  
        };  
        
        /* 
         * createSingleProducer创建一个单生产者的RingBuffer， 
         * 第一个参数叫EventFactory，从名字上理解就是"事件工厂"，其实它的职责就是产生数据填充RingBuffer的区块。 
         * 第二个参数是RingBuffer的大小，它必须是2的指数倍 目的是为了将求模运算转为&运算提高效率 
         */  
        RingBuffer<Trade> ringBuffer = RingBuffer.createSingleProducer(eventFactory, BUFFER_SIZE);  
        //由Sequencer生成，并且包含了已经发布的Sequence的引用，这些的Sequence源于Sequencer和一些独立的消费者的Sequence。它包含了决定是否有供消费者来消费的Event的逻辑
        //创建SequenceBarrier  
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();  
        //创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUMBERS);  
          
//        WorkHandler<Trade> handler = new TradeHandler();  
        
        //WorkerPool：一个WorkProcessor池，其中WorkProcessor将消费Sequence，所以任务可以在实现WorkHandler接口的worker之间移交
        WorkerPool<Trade> workerPool = new WorkerPool<Trade>(ringBuffer, sequenceBarrier, new IgnoreExceptionHandler(),  new TradeHandler());  
          
        workerPool.start(executor);  
          
        //下面这个生产8个数据
        for(int i=0;i<8;i++){  
            long seq=ringBuffer.next();  
            ringBuffer.get(seq).setPrice(Math.random()*9999);  
            ringBuffer.publish(seq);  
        }  
          
        Thread.sleep(1000);  //等上1秒，等消费都处理完成  
        workerPool.halt();  //通知事件(或者说消息)处理器 可以结束了（并不是马上结束!!!）  
        executor.shutdown();  //终止线程池
    }  
}  
