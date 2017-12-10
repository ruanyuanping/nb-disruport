package com.namibank.Disruptor.multi;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WorkerPool;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * 并发 多个生产者 多个消费者实例
 * @author Administrator
 *
 */
public class Main {
	
	public static void main(String[] args) throws Exception {

		//创建ringBuffer
		RingBuffer<Order> ringBuffer = 
									//多个生产者
				RingBuffer.create(ProducerType.MULTI, 
						new EventFactory<Order>() {  
				            @Override  
				            public Order newInstance() {  
				                return new Order();  
				            }  
				        }, 
				        1024 * 1024, 
						new YieldingWaitStrategy());
		
		//创建SequenceBarrier
		SequenceBarrier barriers = ringBuffer.newBarrier();
		
		//三个消费者
		Consumer[] consumers = new Consumer[3];
		for(int i = 0; i < consumers.length; i++){
			consumers[i] = new Consumer("c" + i);
		}
		
		//WorkerPool：一个WorkProcessor池，其中WorkProcessor将消费Sequence，所以任务可以在实现WorkHandler接口的worker之间移交
		WorkerPool<Order> workerPool = 
				new WorkerPool<Order>(ringBuffer, 
						barriers, 
						new IntEventExceptionHandler(),
						consumers);
		
		//把next 所有环形结构下标值添加到ringbuffer
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());  
        
        //启动work池 Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())  获取当前cpu个数
        //workerPool.start(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));  
        //设置线程池
        ExecutorService executors = Executors.newFixedThreadPool(5);
        workerPool.start(executors);  
        
        
        //线程调度器
        final CountDownLatch latch = new CountDownLatch(1);
        
        //循环设置数据
        for (int i = 0; i < 100; i++) {  
        	final Producer p = new Producer(ringBuffer);
        	new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						latch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for(int j = 0; j < 100; j ++){
						p.onData(UUID.randomUUID().toString());
					}
				}
			}).start();
        } 
        Thread.sleep(2000);
        System.out.println("---------------开始生产-----------------");
        //数据准备完毕调用主线程执行任务
        latch.countDown();
        Thread.sleep(5000);
        System.out.println("总数:" + consumers[0].getCount() );
        workerPool.halt();  //关闭事件处理器
        executors.shutdown(); //关闭线程池
	}
	
	static class IntEventExceptionHandler implements ExceptionHandler {  
	    public void handleEventException(Throwable ex, long sequence, Object event) {}  
	    public void handleOnStartException(Throwable ex) {}  
	    public void handleOnShutdownException(Throwable ex) {}  
	} 
}
